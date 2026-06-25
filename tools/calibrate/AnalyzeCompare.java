/*
 * Head-to-head comparison of two pothole-analysis pipelines:
 *
 *   REF  - port of in.potholewatch.domain.cv.PotholeAnalyzer (the reference
 *          code the user pasted). Global threshold + 4-connected flood fill
 *          to find the largest dark blob, then area / contrast / depth.
 *
 *   CUR  - port of com.example.potholereport.ml.PotholeRiskAnalyzer
 *          (the current implementation). Synthetic centered box +
 *          adaptive in-box mask using a baseline ring around the box.
 *
 * Both are run on every JPG in tools/train_pothole_model/dataset/
 * calibration_close_up/ and the per-photo numbers + summary distributions
 * are written to tools/calibrate/analyze_compare.csv plus stdout.
 *
 * Run with the JDK that Gradle uses:
 *   "%JAVA_HOME%\bin\java.exe" tools\calibrate\AnalyzeCompare.java
 */

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AnalyzeCompare {

    public static void main(String[] args) throws Exception {
        Path folder = Paths.get("tools/train_pothole_model/dataset/calibration_close_up");
        Path outDir = Paths.get("tools/calibrate");
        Files.createDirectories(outDir);
        Path csv = outDir.resolve("analyze_compare.csv");

        List<File> files = new ArrayList<>();
        try (var stream = Files.list(folder)) {
            stream.filter(p -> {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
            }).sorted().forEach(p -> files.add(p.toFile()));
        }

        Set<String> seenHashes = new HashSet<>();
        List<RowPair> rows = new ArrayList<>();
        for (File f : files) {
            String hash = quickHash(f);
            if (!seenHashes.add(hash)) continue; // dedup gallery duplicates
            BufferedImage img = ImageIO.read(f);
            if (img == null) continue;
            RowPair rp = new RowPair();
            rp.name = f.getName();
            rp.ref = analyzeReference(img);
            rp.cur = analyzeCurrent(img);
            rp.refRisk = computeRiskReference(rp.ref, /*sev=*/2);
            rows.add(rp);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(csv.toFile()))) {
            pw.println("file,"
                    + "REF_areaPct,REF_areaCm2,REF_depthCm,REF_contrast,REF_riskScore,REF_speedKmh,REF_label,"
                    + "CUR_widthCm,CUR_depthCm,CUR_speedKmh,CUR_label,CUR_widthScore,CUR_depthScore,CUR_maskAreaFrac,CUR_baseLum,CUR_maskMeanLum");
            for (RowPair r : rows) {
                pw.printf(Locale.ROOT,
                        "%s,%.2f,%d,%.2f,%.3f,%d,%d,%s,%d,%d,%d,%s,%.3f,%.3f,%.3f,%.1f,%.1f%n",
                        r.name,
                        r.ref.areaPct, r.ref.areaApproxCm2, r.ref.depthApproxCm, r.ref.contrast,
                        r.refRisk.riskScore, r.refRisk.dangerSpeedKmh, r.refRisk.riskLabel,
                        r.cur.widthCm, r.cur.depthCm, r.cur.speedKmh, r.cur.label,
                        r.cur.widthScore, r.cur.depthScore, r.cur.maskAreaFrac, r.cur.baseLum, r.cur.maskMeanLum);
            }
        }

        printSummary(rows);
        System.out.println("\nFull CSV: " + csv.toAbsolutePath());
    }

    // ====================================================================
    // REFERENCE PIPELINE (port of PotholeAnalyzer.java the user pasted)
    // ====================================================================

    static final int REF_TARGET_EDGE = 240;
    static final double REF_FRAME_AREA_CM2 = 4800;

    static class RefAnalysis {
        boolean valid, detected;
        double areaPct, depthApproxCm, contrast;
        int areaApproxCm2;
    }
    static class RefRisk {
        int riskScore, dangerSpeedKmh;
        String riskLabel;
    }

    static RefAnalysis analyzeReference(BufferedImage source) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        double scale = Math.min(1.0, (double) REF_TARGET_EDGE / Math.max(srcW, srcH));
        int w = Math.max(1, (int) Math.round(srcW * scale));
        int h = Math.max(1, (int) Math.round(srcH * scale));
        BufferedImage small = resize(source, w, h);

        int total = w * h;
        int[] pixels = new int[total];
        small.getRGB(0, 0, w, h, pixels, 0, w);

        int[] gray = new int[total];
        double brightSum = 0;
        for (int i = 0; i < total; i++) {
            int px = pixels[i];
            int r = (px >> 16) & 0xFF;
            int g = (px >> 8)  & 0xFF;
            int b = px & 0xFF;
            int lum = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            gray[i] = lum;
            brightSum += lum;
        }
        double meanBrightness = brightSum / total;

        double varSum = 0;
        for (int i = 0; i < total; i++) {
            double d = gray[i] - meanBrightness;
            varSum += d * d;
        }
        double stddev = Math.sqrt(varSum / total);
        int threshold = (int) Math.max(20, meanBrightness - 0.6 * stddev);

        boolean[] mask = new boolean[total];
        for (int i = 0; i < total; i++) mask[i] = gray[i] < threshold;

        boolean[] visited = new boolean[total];
        int[] stack = new int[total];
        int bestSize = 0;
        double bestSum = 0;

        for (int start = 0; start < total; start++) {
            if (!mask[start] || visited[start]) continue;
            int sp = 0;
            stack[sp++] = start;
            visited[start] = true;
            int size = 0;
            double darkSum = 0;
            while (sp > 0) {
                int ci = stack[--sp];
                size++;
                darkSum += gray[ci];
                int cy = ci / w;
                int cx = ci - cy * w;
                if (cy > 0)     { int n = ci - w; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n; } }
                if (cy < h - 1) { int n = ci + w; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n; } }
                if (cx > 0)     { int n = ci - 1; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n; } }
                if (cx < w - 1) { int n = ci + 1; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n; } }
            }
            if (size > bestSize) {
                bestSize = size;
                bestSum = darkSum;
            }
        }

        double areaPct = ((double) bestSize / total) * 100.0;
        RefAnalysis a = new RefAnalysis();
        if (bestSize < 30 || areaPct < 0.5) {
            a.valid = false;
            return a;
        }
        double componentMean = bestSum / bestSize;
        double nonPotholeCount = Math.max(1, total - bestSize);
        double nonPotholeMean = (brightSum - bestSum) / nonPotholeCount;
        if (nonPotholeMean <= 0) nonPotholeMean = meanBrightness;

        double contrast = Math.max(0, (nonPotholeMean - componentMean) / Math.max(1, nonPotholeMean));
        int areaApproxCm2 = (int) Math.round((areaPct / 100.0) * REF_FRAME_AREA_CM2);
        double depthApproxCm = Math.round(Math.max(0.5, Math.min(15, contrast * 22)) * 10.0) / 10.0;

        a.valid = true;
        a.detected = true;
        a.areaPct = Math.round(areaPct * 10.0) / 10.0;
        a.areaApproxCm2 = areaApproxCm2;
        a.depthApproxCm = depthApproxCm;
        a.contrast = Math.round(contrast * 100.0) / 100.0;
        return a;
    }

    static RefRisk computeRiskReference(RefAnalysis ra, int userSeverity) {
        int sev = userSeverity >= 1 && userSeverity <= 4 ? userSeverity : 2;
        if (!ra.valid || !ra.detected) {
            ra = synthFromSeverity(sev);
        }
        double depthScore    = Math.min(100, ra.depthApproxCm * 10);
        double areaScore     = Math.min(100, ra.areaPct * 4);
        double contrastScore = Math.min(100, ra.contrast * 180);
        double severityScore = sev * 25;
        int riskScore = (int) Math.round(
                depthScore * 0.30 + areaScore * 0.25 + contrastScore * 0.15 + severityScore * 0.30);
        RefRisk r = new RefRisk();
        r.riskScore = riskScore;
        if (riskScore >= 75)      { r.dangerSpeedKmh = 20; r.riskLabel = "CRITICAL"; }
        else if (riskScore >= 55) { r.dangerSpeedKmh = 30; r.riskLabel = "HIGH"; }
        else if (riskScore >= 35) { r.dangerSpeedKmh = 45; r.riskLabel = "MODERATE"; }
        else                      { r.dangerSpeedKmh = 60; r.riskLabel = "LOW"; }
        return r;
    }

    static RefAnalysis synthFromSeverity(int sev) {
        RefAnalysis a = new RefAnalysis();
        a.valid = true; a.detected = true;
        switch (sev) {
            case 1: a.areaPct = 2;  a.areaApproxCm2 = 100;  a.depthApproxCm = 1.2; a.contrast = 0.18; break;
            case 3: a.areaPct = 16; a.areaApproxCm2 = 950;  a.depthApproxCm = 6.5; a.contrast = 0.48; break;
            case 4: a.areaPct = 28; a.areaApproxCm2 = 1800; a.depthApproxCm = 10.5;a.contrast = 0.62; break;
            default: a.areaPct = 8; a.areaApproxCm2 = 400;  a.depthApproxCm = 3.5; a.contrast = 0.32; break;
        }
        return a;
    }

    // ====================================================================
    // CURRENT PIPELINE (port of PotholeRiskAnalyzer.kt, synthetic box path)
    // ====================================================================

    static class CurAnalysis {
        int widthCm, depthCm, speedKmh;
        String label;
        float widthScore, depthScore, maskAreaFrac;
        float baseLum, maskMeanLum;
    }

    static final int CUR_MAX_SIDE = 960;

    static CurAnalysis analyzeCurrent(BufferedImage source) {
        BufferedImage scaled = scaleToMaxSide(source, CUR_MAX_SIDE);
        int w = scaled.getWidth();
        int h = scaled.getHeight();
        int[] px = new int[w * h];
        scaled.getRGB(0, 0, w, h, px, 0, w);

        // Synthetic box: middle 70%
        int x0 = Math.round(0.15f * w);
        int y0 = Math.round(0.15f * h);
        int x1 = Math.round(0.85f * w);
        int y1 = Math.round(0.85f * h);

        // Sample baseline ring around the box
        int boxW = x1 - x0, boxH = y1 - y0;
        int pad = Math.max(6, Math.round(Math.max(boxW, boxH) * 0.20f));
        int rx0 = Math.max(0, x0 - pad), ry0 = Math.max(0, y0 - pad);
        int rx1 = Math.min(w, x1 + pad), ry1 = Math.min(h, y1 + pad);
        float baseSum = 0, baseSumSq = 0; int baseN = 0;
        for (int yy = ry0; yy < ry1; yy++) {
            for (int xx = rx0; xx < rx1; xx++) {
                if (xx >= x0 && xx < x1 && yy >= y0 && yy < y1) continue;
                float lum = lumOf(px, w, xx, yy);
                baseSum += lum; baseSumSq += lum * lum; baseN++;
            }
        }
        float baseLum = baseN > 0 ? baseSum / baseN : 110f;
        float baseStd = baseN > 0
                ? (float) Math.sqrt(Math.max(0, baseSumSq / baseN - baseLum * baseLum))
                : 32f;
        if (baseStd < 8f) baseStd = 8f;
        if (baseLum < 40f) baseLum = 40f;
        float darknessThreshold = Math.max(15f, baseLum - 0.65f * baseStd);

        boolean[] mask = new boolean[boxW * boxH];
        int maskCount = 0;
        float sumDarkLum = 0;
        int darkCount = 0;
        for (int yy = 0; yy < boxH; yy++) {
            int py = y0 + yy;
            for (int xx = 0; xx < boxW; xx++) {
                int p = px[py * w + (x0 + xx)];
                int rr = (p >> 16) & 0xFF, gg = (p >> 8) & 0xFF, bb = p & 0xFF;
                float lum = 0.299f * rr + 0.587f * gg + 0.114f * bb;
                int maxC = Math.max(rr, Math.max(gg, bb));
                int minC = Math.min(rr, Math.min(gg, bb));
                float sat = maxC == 0 ? 0f : (maxC - minC) / (float) maxC;
                if (lum < darknessThreshold && sat < 0.55f) {
                    mask[yy * boxW + xx] = true;
                    maskCount++;
                    sumDarkLum += lum;
                    if (lum < darknessThreshold - 0.3f * baseStd) darkCount++;
                }
            }
        }

        CurAnalysis a = new CurAnalysis();
        a.baseLum = baseLum;
        if (maskCount < 8) {
            a.maskAreaFrac = 0f;
            a.widthScore = 0.20f;
            a.depthScore = 0.20f;
        } else {
            float maskAreaFrac = maskCount / (float) (boxW * boxH);
            a.maskAreaFrac = maskAreaFrac;
            a.maskMeanLum = sumDarkLum / maskCount;

            // Width scoring (synthetic box always 0.7×0.7=0.49 area, width 0.7).
            float area = (boxW * boxH) / (float) (w * h);
            float width = boxW / (float) w;
            float maskFootprint = Math.max(0f, Math.min(1f, maskAreaFrac * area));
            float roadRatio = 0.6f; // not measured here; constant placeholder
            a.widthScore = Math.max(0f, Math.min(1f,
                    area * 0.34f + width * 0.20f + maskFootprint * 0.30f + roadRatio * 0.10f));

            // Depth scoring
            float darkFrac = darkCount / (float) maskCount;
            float contrast = Math.max(0f, Math.min(1f, (baseLum - a.maskMeanLum) / baseLum));
            a.depthScore = Math.max(0f, Math.min(1f, contrast * 0.55f + darkFrac * 0.30f));
        }

        a.widthCm = widthScoreToCm(a.widthScore);
        a.depthCm = depthScoreToCm(a.depthScore);

        String severity;
        if (a.depthCm >= 18 || a.widthCm >= 100) { severity = "CRITICAL"; a.speedKmh = 10; a.label = "EXTREME"; }
        else if (a.depthCm >= 12 || a.widthCm >= 75) { severity = "SEVERE"; a.speedKmh = 15; a.label = "HIGH"; }
        else if (a.depthCm >= 7 || a.widthCm >= 45) { severity = "MODERATE"; a.speedKmh = 25; a.label = "MEDIUM"; }
        else { severity = "MINOR"; a.speedKmh = 40; a.label = "LOW"; }
        return a;
    }

    static int widthScoreToCm(float score) {
        if (score < 0.18f) return 20;
        if (score < 0.30f) return 35;
        if (score < 0.45f) return 50;
        if (score < 0.60f) return 70;
        if (score < 0.75f) return 95;
        return 120;
    }

    static int depthScoreToCm(float score) {
        if (score < 0.20f) return 3;
        if (score < 0.35f) return 6;
        if (score < 0.50f) return 9;
        if (score < 0.65f) return 13;
        if (score < 0.80f) return 18;
        return 24;
    }

    // ====================================================================
    // helpers / summary
    // ====================================================================

    static class RowPair {
        String name;
        RefAnalysis ref;
        RefRisk refRisk;
        CurAnalysis cur;
    }

    static void printSummary(List<RowPair> rows) {
        System.out.println("===== Head-to-head: REFERENCE vs CURRENT =====");
        System.out.println("Photos compared: " + rows.size() + " (deduplicated)");

        // REF detection rate
        int refDetected = 0;
        for (RowPair r : rows) if (r.ref.valid && r.ref.detected) refDetected++;
        System.out.println("\nREFERENCE detection rate: " + refDetected + " / " + rows.size());

        // REF risk-label distribution
        var refLabels = new java.util.LinkedHashMap<String, Integer>();
        for (RowPair r : rows) refLabels.merge(r.refRisk.riskLabel, 1, Integer::sum);
        System.out.println("\nREFERENCE risk label distribution (with userSeverity=2):");
        refLabels.forEach((k, v) -> System.out.printf(Locale.ROOT, "  %-10s %3d%n", k, v));

        // CUR severity-label distribution
        var curLabels = new java.util.LinkedHashMap<String, Integer>();
        for (RowPair r : rows) curLabels.merge(r.cur.label, 1, Integer::sum);
        System.out.println("\nCURRENT severity label distribution:");
        curLabels.forEach((k, v) -> System.out.printf(Locale.ROOT, "  %-10s %3d%n", k, v));

        // Number-by-number stats
        printStat("REF areaPct (%)",   rows, r -> (float) r.ref.areaPct);
        printStat("REF depthCm",       rows, r -> (float) r.ref.depthApproxCm);
        printStat("REF contrast",      rows, r -> (float) r.ref.contrast);
        printStat("REF riskScore",     rows, r -> (float) r.refRisk.riskScore);
        printStat("REF speedKmh",      rows, r -> (float) r.refRisk.dangerSpeedKmh);

        printStat("CUR widthCm",       rows, r -> (float) r.cur.widthCm);
        printStat("CUR depthCm",       rows, r -> (float) r.cur.depthCm);
        printStat("CUR speedKmh",      rows, r -> (float) r.cur.speedKmh);
        printStat("CUR widthScore",    rows, r -> r.cur.widthScore);
        printStat("CUR depthScore",    rows, r -> r.cur.depthScore);
        printStat("CUR maskAreaFrac",  rows, r -> r.cur.maskAreaFrac);
    }

    interface Sel { float pick(RowPair r); }

    static void printStat(String label, List<RowPair> rows, Sel sel) {
        if (rows.isEmpty()) return;
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        double sum = 0;
        float[] vals = new float[rows.size()];
        int i = 0;
        for (RowPair r : rows) {
            float v = sel.pick(r);
            vals[i++] = v;
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        Arrays.sort(vals);
        float median = vals[vals.length / 2];
        float p10 = vals[Math.max(0, (int) (vals.length * 0.10))];
        float p90 = vals[Math.min(vals.length - 1, (int) (vals.length * 0.90))];
        System.out.printf(Locale.ROOT,
                "%n  %-20s  min=%.2f  p10=%.2f  med=%.2f  p90=%.2f  max=%.2f  mean=%.2f",
                label, min, p10, median, p90, max, sum / rows.size());
    }

    static float lumOf(int[] px, int w, int x, int y) {
        int c = px[y * w + x];
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        return 0.299f * r + 0.587f * g + 0.114f * b;
    }

    static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    static BufferedImage scaleToMaxSide(BufferedImage src, int maxSide) {
        int sw = src.getWidth(), sh = src.getHeight();
        int max = Math.max(sw, sh);
        if (max <= maxSide) return src;
        double s = (double) maxSide / max;
        return resize(src, Math.max(1, (int) Math.round(sw * s)), Math.max(1, (int) Math.round(sh * s)));
    }

    static String quickHash(File f) throws Exception {
        byte[] head = new byte[Math.min(65536, (int) f.length())];
        try (var in = Files.newInputStream(f.toPath())) {
            int r = in.read(head);
            if (r < head.length) head = Arrays.copyOf(head, Math.max(0, r));
        }
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(Long.toString(f.length()).getBytes());
        md.update(head);
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
