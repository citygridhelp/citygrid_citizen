/*
 * Run the NEW PotholeRiskAnalyzer pipeline (blob finder + reference-style
 * formulas + 60/45/30/20 km/h speeds) against the 34 unique calibration
 * close-ups for each lane (left/middle/right) and print per-photo
 * width/depth/severity/speed numbers. The point is to confirm that the
 * "always 50 cm wide / always MEDIUM severity" symptom is gone.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AnalyzeNew {

    static final int TARGET_EDGE = 240;
    static final float APPROX_FRAME_AREA_CM2 = 4800f;

    public static void main(String[] args) throws Exception {
        Path positives = Paths.get("tools/train_pothole_model/dataset/calibration_close_up");
        List<File> files = new ArrayList<>();
        try (var s = Files.list(positives)) {
            s.filter(p -> {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
            }).sorted().forEach(p -> files.add(p.toFile()));
        }
        Set<String> seen = new HashSet<>();
        List<File> uniq = new ArrayList<>();
        for (File f : files) if (seen.add(quickHash(f))) uniq.add(f);

        Path csv = Paths.get("tools/calibrate/analyze_new.csv");
        PrintWriter pw = new PrintWriter(new FileWriter(csv.toFile()));
        pw.println("file,lane,detected,areaPct,contrast,widthCm,depthCm,riskScore,severity,speedKmh,label");

        Map<String, int[]> labelCounts = new LinkedHashMap<>();
        labelCounts.put("LEFT", new int[]{0, 0, 0, 0});
        labelCounts.put("MIDDLE", new int[]{0, 0, 0, 0});
        labelCounts.put("RIGHT", new int[]{0, 0, 0, 0});

        for (File f : uniq) {
            BufferedImage img = ImageIO.read(f);
            if (img == null) continue;
            for (Lane lane : Lane.values()) {
                Result r = analyze(img, lane, /*sev=*/2);
                pw.printf(Locale.ROOT, "%s,%s,%s,%.2f,%.3f,%d,%d,%d,%s,%d,%s%n",
                        f.getName(), lane.name(), r.detected, r.areaPct, r.contrast,
                        r.widthCm, r.depthCm, r.riskScore, r.severity, r.speedKmh, r.label);
                labelCounts.get(lane.name())[r.bucket]++;
            }
        }
        pw.close();

        System.out.println("===== NEW analyzer label distribution per lane =====");
        System.out.printf("%-8s %8s %8s %8s %8s%n", "lane", "LOW", "MOD", "HIGH", "CRIT");
        for (var e : labelCounts.entrySet()) {
            int[] cs = e.getValue();
            System.out.printf("%-8s %8d %8d %8d %8d%n", e.getKey(), cs[0], cs[1], cs[2], cs[3]);
        }

        // Width/depth distribution for the MIDDLE lane (the typical default).
        List<Integer> widths = new ArrayList<>(), depths = new ArrayList<>();
        for (File f : uniq) {
            BufferedImage img = ImageIO.read(f);
            if (img == null) continue;
            Result r = analyze(img, Lane.MIDDLE, 2);
            if (r.detected) { widths.add(r.widthCm); depths.add(r.depthCm); }
        }
        System.out.printf("%nMIDDLE lane width cm:  min=%d  med=%d  max=%d  distinct=%d%n",
                min(widths), median(widths), max(widths), distinct(widths));
        System.out.printf("MIDDLE lane depth cm:  min=%d  med=%d  max=%d  distinct=%d%n",
                min(depths), median(depths), max(depths), distinct(depths));
        System.out.println("\nFull CSV: " + csv.toAbsolutePath());
    }

    enum Lane {
        LEFT(0f, 0.42f), MIDDLE(0.30f, 0.70f), RIGHT(0.58f, 1f);
        final float start, end;
        Lane(float s, float e) { start = s; end = e; }
    }

    static class Result {
        boolean detected;
        double areaPct, contrast;
        int widthCm, depthCm, riskScore, speedKmh, bucket;
        String severity, label;
    }

    static Result analyze(BufferedImage source, Lane lane, int userSeverity) {
        Result r = new Result();
        int srcW = source.getWidth(), srcH = source.getHeight();
        double scale = Math.min(1.0, (double) TARGET_EDGE / Math.max(srcW, srcH));
        int w = Math.max(1, (int) Math.round(srcW * scale));
        int h = Math.max(1, (int) Math.round(srcH * scale));
        BufferedImage small = resize(source, w, h);
        int total = w * h;
        int[] px = new int[total];
        small.getRGB(0, 0, w, h, px, 0, w);

        int[] gray = new int[total];
        double brightSum = 0;
        for (int i = 0; i < total; i++) {
            int p = px[i];
            int rr = (p >> 16) & 0xFF, gg = (p >> 8) & 0xFF, bb = p & 0xFF;
            int lum = (int) (0.299 * rr + 0.587 * gg + 0.114 * bb);
            gray[i] = lum;
            brightSum += lum;
        }
        double mean = brightSum / total;
        double var = 0;
        for (int i = 0; i < total; i++) { double d = gray[i] - mean; var += d * d; }
        double std = Math.sqrt(var / total);
        int threshold = (int) Math.max(20, mean - 0.6 * std);

        int laneStart = (int) Math.round(lane.start * w);
        int laneEnd = (int) Math.round(lane.end * w);

        boolean[] mask = new boolean[total];
        for (int i = 0; i < total; i++) mask[i] = gray[i] < threshold;

        boolean[] visited = new boolean[total];
        int[] stack = new int[total];
        int bestSize = 0;
        double bestSum = 0;
        int bx0 = 0, by0 = 0, bx1 = 0, by1 = 0;

        for (int y = 0; y < h; y++) {
            for (int x = laneStart; x < laneEnd; x++) {
                int start = y * w + x;
                if (!mask[start] || visited[start]) continue;
                int sp = 0; stack[sp++] = start; visited[start] = true;
                int size = 0; double darkSum = 0;
                int minX = w, minY = h, maxX = 0, maxY = 0;
                while (sp > 0) {
                    int ci = stack[--sp];
                    size++; darkSum += gray[ci];
                    int cy = ci / w, cx = ci - cy * w;
                    if (cx < minX) minX = cx;
                    if (cy < minY) minY = cy;
                    if (cx > maxX) maxX = cx;
                    if (cy > maxY) maxY = cy;
                    if (cy > 0)     { int n = ci - w; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n; } }
                    if (cy < h - 1) { int n = ci + w; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n; } }
                    if (cx > 0)     { int n = ci - 1; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n; } }
                    if (cx < w - 1) { int n = ci + 1; if (mask[n] && !visited[n]) { visited[n] = true; stack[sp++] = n; } }
                }
                if (size > bestSize) {
                    bestSize = size; bestSum = darkSum;
                    bx0 = minX; by0 = minY; bx1 = maxX; by1 = maxY;
                }
            }
        }

        if (bestSize < 30) {
            r.detected = false;
            return r;
        }
        double areaPct = ((double) bestSize / total) * 100.0;
        if (areaPct < 0.5) {
            r.detected = false;
            return r;
        }
        double componentMean = bestSum / bestSize;
        double nonBlobCount = Math.max(1, total - bestSize);
        double nonBlobMean = (brightSum - bestSum) / nonBlobCount;
        if (nonBlobMean <= 0) nonBlobMean = mean;
        double contrast = Math.max(0,
                (nonBlobMean - componentMean) / Math.max(1, nonBlobMean));

        int boxW = Math.max(1, bx1 - bx0 + 1);
        int boxH = Math.max(1, by1 - by0 + 1);
        float aspect = Math.max(boxW, boxH) / (float) Math.min(boxW, boxH);

        // Apply same width / depth derivation as the Kotlin analyzer.
        double areaCm2 = Math.max(20, (areaPct / 100.0) * APPROX_FRAME_AREA_CM2);
        double widthCm = Math.sqrt(areaCm2) * Math.sqrt(Math.max(1, Math.min(4, aspect)));
        widthCm = Math.max(8, Math.min(180, widthCm));
        double depthCm = Math.max(0.5, Math.min(18, contrast * 22));

        // Risk score (4-signal weighted).
        double depthScore = Math.min(100, depthCm * 10);
        double areaScore = Math.min(100, areaPct * 4);
        double contrastScore = Math.min(100, contrast * 180);
        double severityScore = userSeverity * 25.0;
        int riskScore = (int) Math.round(depthScore * 0.30
                + areaScore * 0.25
                + contrastScore * 0.15
                + severityScore * 0.30);

        r.detected = true;
        r.areaPct = areaPct;
        r.contrast = contrast;
        r.widthCm = (int) Math.round(widthCm);
        r.depthCm = (int) Math.round(depthCm);
        r.riskScore = riskScore;
        if (riskScore >= 75)      { r.severity = "CRITICAL"; r.label = "CRITICAL"; r.speedKmh = 20; r.bucket = 3; }
        else if (riskScore >= 55) { r.severity = "SEVERE";   r.label = "HIGH";     r.speedKmh = 30; r.bucket = 2; }
        else if (riskScore >= 35) { r.severity = "MODERATE"; r.label = "MODERATE"; r.speedKmh = 45; r.bucket = 1; }
        else                      { r.severity = "MINOR";    r.label = "LOW";      r.speedKmh = 60; r.bucket = 0; }
        return r;
    }

    static int min(List<Integer> v) { return v.stream().min(Integer::compareTo).orElse(0); }
    static int max(List<Integer> v) { return v.stream().max(Integer::compareTo).orElse(0); }
    static int median(List<Integer> v) {
        if (v.isEmpty()) return 0;
        var sorted = new ArrayList<>(v);
        sorted.sort(Integer::compareTo);
        return sorted.get(sorted.size() / 2);
    }
    static int distinct(List<Integer> v) { return (int) v.stream().distinct().count(); }

    static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    static String quickHash(File f) throws Exception {
        byte[] head = new byte[Math.min(65536, (int) f.length())];
        try (var in = Files.newInputStream(f.toPath())) {
            int rr = in.read(head);
            if (rr < head.length) head = Arrays.copyOf(head, Math.max(0, rr));
        }
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(Long.toString(f.length()).getBytes());
        md.update(head);
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
