/*
 * Run the reference-style "darkest connected blob" finder on EVERY photo we
 * have - 34 unique positive close-ups and the 53 negatives - and report the
 * distribution of (areaPct, contrast, aspect, edgeTouches) per category.
 *
 * The hypothesis: a real pothole shows up as a large, high-contrast dark
 * blob, while manholes / walls / cement pipes / sign boards / trees do not.
 * If that is true, a couple of thresholds on (areaPct, contrast) will
 * cleanly separate positives from negatives without needing dozens of
 * category-specific rules.
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

public class BlobSurvey {

    static final int TARGET_EDGE = 240;

    public static void main(String[] args) throws Exception {
        Path positives = Paths.get("tools/train_pothole_model/dataset/calibration_close_up");
        Path negativesRoot = Paths.get("tools/train_pothole_model/dataset/Pothole Negative samples");

        Map<String, List<File>> categories = new LinkedHashMap<>();

        // Positives -> single virtual category "POSITIVE pothole"
        List<File> posFiles = new ArrayList<>();
        try (var s = Files.list(positives)) {
            s.filter(p -> {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
            }).sorted().forEach(p -> posFiles.add(p.toFile()));
        }
        categories.put("POSITIVE pothole", dedup(posFiles));

        // Negatives -> per subfolder
        try (var s = Files.list(negativesRoot)) {
            for (Path cat : s.filter(Files::isDirectory).sorted().toArray(Path[]::new)) {
                List<File> files = new ArrayList<>();
                try (var s2 = Files.list(cat)) {
                    s2.filter(p -> {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
                    }).sorted().forEach(p -> files.add(p.toFile()));
                }
                categories.put("NEGATIVE " + cat.getFileName(), files);
            }
        }

        Path outDir = Paths.get("tools/calibrate");
        Files.createDirectories(outDir);
        Path csv = outDir.resolve("blob_survey.csv");
        PrintWriter pw = new PrintWriter(new FileWriter(csv.toFile()));
        pw.println("category,file,detected,areaPct,contrast,aspect,edgeTouches,depthCm,"
                + "ringAsphaltFrac,blobInsideAsphalt,bbox_x0,bbox_y0,bbox_x1,bbox_y1");

        Map<String, List<Blob>> per = new LinkedHashMap<>();
        for (var e : categories.entrySet()) {
            List<Blob> rs = new ArrayList<>();
            for (File f : e.getValue()) {
                BufferedImage img = ImageIO.read(f);
                if (img == null) continue;
                Blob b = findLargestDarkBlob(img);
                rs.add(b);
                pw.printf(Locale.ROOT, "%s,%s,%s,%.3f,%.3f,%.3f,%d,%.2f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                        e.getKey(), f.getName(), b.detected, b.areaPct, b.contrast, b.aspect,
                        b.edgeTouches, b.depthCm, b.ringAsphaltFrac, b.blobInsideAsphalt,
                        b.bx0, b.by0, b.bx1, b.by1);
            }
            per.put(e.getKey(), rs);
        }
        pw.close();

        printSummary(per);
        proposeThresholds(per);
        evaluateValidatorThresholds(per);
        System.out.println("\nFull CSV: " + csv.toAbsolutePath());
    }

    /**
     * Evaluate the EXACT thresholds shipping in PotholePhotoValidator.kt so
     * we know how many positives the validator will drop and how many
     * negatives it will catch in production.
     */
    static void evaluateValidatorThresholds(Map<String, List<Blob>> per) {
        final float MAX_AREA = 30.0f;
        final float MAX_CONTRAST = 0.80f;
        final float MID_AREA = 18.0f;
        final float MID_INSIDE = 0.50f;
        final float HARD_INSIDE = 0.18f;
        final float HARD_AREA_FLOOR = 4.0f;

        System.out.println("\n===== Validator (shipping) thresholds =====");
        System.out.printf(Locale.ROOT,
                "Reject if  areaPct >= %.1f  OR  contrast >= %.2f%n"
                + "       OR (areaPct >= %.1f AND insideAsphalt < %.2f)%n"
                + "       OR (insideAsphalt < %.2f AND areaPct >= %.1f)%n",
                MAX_AREA, MAX_CONTRAST, MID_AREA, MID_INSIDE, HARD_INSIDE, HARD_AREA_FLOOR);
        for (var e : per.entrySet()) {
            int kept = 0;
            int total = e.getValue().size();
            for (Blob b : e.getValue()) {
                if (!b.detected) { kept++; continue; }
                boolean reject =
                        b.areaPct >= MAX_AREA
                     || b.contrast >= MAX_CONTRAST
                     || (b.areaPct >= MID_AREA && b.blobInsideAsphalt < MID_INSIDE)
                     || (b.blobInsideAsphalt < HARD_INSIDE && b.areaPct >= HARD_AREA_FLOOR);
                if (!reject) kept++;
            }
            String want = e.getKey().startsWith("POSITIVE") ? "want kept" : "want rejected";
            System.out.printf(Locale.ROOT, "  %-30s  %d / %d kept  (%s)%n",
                    e.getKey(), kept, total, want);
        }
    }

    static List<File> dedup(List<File> in) throws Exception {
        Set<String> hashes = new HashSet<>();
        List<File> out = new ArrayList<>();
        for (File f : in) if (hashes.add(quickHash(f))) out.add(f);
        return out;
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

    // ===================== blob finder =====================

    static class Blob {
        boolean detected;
        double areaPct, contrast, depthCm;
        float aspect, bx0, by0, bx1, by1;
        int edgeTouches;
        float ringAsphaltFrac;   // % of ring around blob bbox classified asphalt
        float blobInsideAsphalt; // % of blob's pixels classified asphalt
    }

    static Blob findLargestDarkBlob(BufferedImage source) {
        int srcW = source.getWidth(), srcH = source.getHeight();
        double scale = Math.min(1.0, (double) TARGET_EDGE / Math.max(srcW, srcH));
        int w = Math.max(1, (int) Math.round(srcW * scale));
        int h = Math.max(1, (int) Math.round(srcH * scale));
        BufferedImage small = resize(source, w, h);
        int total = w * h;
        int[] pixels = new int[total];
        small.getRGB(0, 0, w, h, pixels, 0, w);

        int[] gray = new int[total];
        double brightSum = 0;
        boolean[] asphalt = new boolean[total];
        for (int i = 0; i < total; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            int lum = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            gray[i] = lum;
            brightSum += lum;
            int maxC = Math.max(r, Math.max(g, b));
            int minC = Math.min(r, Math.min(g, b));
            float sat = maxC == 0 ? 0f : (maxC - minC) / (float) maxC;
            // Same isAsphalt rule as SceneHeuristics.
            asphalt[i] = lum >= 35 && lum <= 190 && sat < 0.42f
                    && Math.abs(r - g) < 45 && Math.abs(g - b) < 45;
        }
        double mean = brightSum / total;
        double var = 0;
        for (int i = 0; i < total; i++) {
            double d = gray[i] - mean;
            var += d * d;
        }
        double std = Math.sqrt(var / total);
        int threshold = (int) Math.max(20, mean - 0.6 * std);

        boolean[] mask = new boolean[total];
        for (int i = 0; i < total; i++) mask[i] = gray[i] < threshold;

        boolean[] visited = new boolean[total];
        boolean[] bestMask = new boolean[total];
        int[] stack = new int[total];
        int bestSize = 0;
        double bestSum = 0;
        int bx0 = 0, by0 = 0, bx1 = 0, by1 = 0;

        boolean[] currentMask = new boolean[total];
        for (int start = 0; start < total; start++) {
            if (!mask[start] || visited[start]) continue;
            int sp = 0;
            stack[sp++] = start;
            visited[start] = true;
            int size = 0;
            double darkSum = 0;
            int minX = w, minY = h, maxX = 0, maxY = 0;
            // Reuse currentMask: only positions touched will need clearing.
            int[] touched = new int[total];
            int touchedCount = 0;
            while (sp > 0) {
                int ci = stack[--sp];
                size++;
                darkSum += gray[ci];
                currentMask[ci] = true;
                touched[touchedCount++] = ci;
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
                bestSize = size;
                bestSum = darkSum;
                bx0 = minX; by0 = minY; bx1 = maxX; by1 = maxY;
                // Promote currentMask to bestMask.
                System.arraycopy(currentMask, 0, bestMask, 0, total);
            }
            // Clear currentMask for next component.
            for (int t = 0; t < touchedCount; t++) currentMask[touched[t]] = false;
        }

        Blob result = new Blob();
        if (bestSize < 30) {
            result.detected = false;
            return result;
        }
        double areaPct = ((double) bestSize / total) * 100.0;
        double componentMean = bestSum / bestSize;
        double nonPotholeCount = Math.max(1, total - bestSize);
        double nonPotholeMean = (brightSum - bestSum) / nonPotholeCount;
        if (nonPotholeMean <= 0) nonPotholeMean = mean;
        double contrast = Math.max(0, (nonPotholeMean - componentMean) / Math.max(1, nonPotholeMean));

        int edgeTouches = 0;
        if (bx0 == 0) edgeTouches++;
        if (by0 == 0) edgeTouches++;
        if (bx1 == w - 1) edgeTouches++;
        if (by1 == h - 1) edgeTouches++;

        int boxW = Math.max(1, bx1 - bx0 + 1);
        int boxH = Math.max(1, by1 - by0 + 1);
        float aspect = Math.max(boxW, boxH) / (float) Math.min(boxW, boxH);

        // Asphalt fraction inside the blob (should be near 0 for real
        // potholes -- the hole isn't asphalt-classified) and in a ring
        // around the bbox (should be high for real potholes -- they're
        // surrounded by road). For walls/pipes/trees the ring won't be
        // asphalt because the negative object dominates the frame.
        int padX = Math.max(2, boxW / 4);
        int padY = Math.max(2, boxH / 4);
        int rx0 = Math.max(0, bx0 - padX);
        int ry0 = Math.max(0, by0 - padY);
        int rx1 = Math.min(w - 1, bx1 + padX);
        int ry1 = Math.min(h - 1, by1 + padY);
        int ringAsphalt = 0, ringN = 0;
        for (int yy = ry0; yy <= ry1; yy++) {
            for (int xx = rx0; xx <= rx1; xx++) {
                if (xx >= bx0 && xx <= bx1 && yy >= by0 && yy <= by1) continue;
                ringN++;
                if (asphalt[yy * w + xx]) ringAsphalt++;
            }
        }
        int blobAsphalt = 0;
        for (int i = 0; i < total; i++) if (bestMask[i] && asphalt[i]) blobAsphalt++;

        result.detected = true;
        result.areaPct = areaPct;
        result.contrast = contrast;
        result.depthCm = Math.round(Math.max(0.5, Math.min(15, contrast * 22)) * 10.0) / 10.0;
        result.aspect = aspect;
        result.edgeTouches = edgeTouches;
        result.bx0 = bx0 / (float) w;
        result.by0 = by0 / (float) h;
        result.bx1 = bx1 / (float) w;
        result.by1 = by1 / (float) h;
        result.ringAsphaltFrac = ringN > 0 ? ringAsphalt / (float) ringN : 0f;
        result.blobInsideAsphalt = bestSize > 0 ? blobAsphalt / (float) bestSize : 0f;
        return result;
    }

    // ===================== summary + threshold proposals =====================

    static void printSummary(Map<String, List<Blob>> per) {
        System.out.println("===== Blob-finder results per category =====");
        for (var e : per.entrySet()) {
            String cat = e.getKey();
            List<Blob> rs = e.getValue();
            int detected = 0;
            for (Blob b : rs) if (b.detected) detected++;
            System.out.printf(Locale.ROOT, "%n[%s]  %d photos  detected=%d%n", cat, rs.size(), detected);
            stat("areaPct (%)",       rs, b -> (float) b.areaPct);
            stat("contrast",          rs, b -> (float) b.contrast);
            stat("depthCm",           rs, b -> (float) b.depthCm);
            stat("aspect",            rs, b -> b.aspect);
            stat("edgeTouches",       rs, b -> (float) b.edgeTouches);
            stat("ringAsphaltFrac",   rs, b -> b.ringAsphaltFrac);
            stat("blobInsideAsphalt", rs, b -> b.blobInsideAsphalt);
        }
    }

    interface Sel { float pick(Blob b); }
    static void stat(String label, List<Blob> rs, Sel sel) {
        if (rs.isEmpty()) return;
        float[] vals = new float[rs.size()];
        int i = 0;
        for (Blob b : rs) vals[i++] = sel.pick(b);
        Arrays.sort(vals);
        float min = vals[0], max = vals[vals.length - 1];
        float median = vals[vals.length / 2];
        double sum = 0;
        for (float v : vals) sum += v;
        System.out.printf(Locale.ROOT, "    %-14s  min=%.2f  med=%.2f  max=%.2f  mean=%.2f%n",
                label, min, median, max, sum / vals.length);
    }

    /**
     * Compound rejection sweep with blobInsideAsphalt added. Reject if:
     *
     *   areaPct >= maxArea
     *   OR contrast >= maxContrast
     *   OR (areaPct >= midArea AND blobInsideAsphalt < midInsideAsphalt)
     *   OR blobInsideAsphalt < hardInsideAsphalt
     */
    static void proposeThresholds(Map<String, List<Blob>> per) {
        List<Blob> pos = per.get("POSITIVE pothole");
        if (pos == null) return;
        List<Blob> neg = new ArrayList<>();
        for (var e : per.entrySet()) if (e.getKey().startsWith("NEGATIVE ")) neg.addAll(e.getValue());

        System.out.println("\n===== Compound-rule rejection sweep (with blobInsideAsphalt) =====");
        float bestNegR = -1f;
        float bMaxArea = 0, bMaxContrast = 0, bMidArea = 0, bMidInside = 0, bHardInside = 0;
        int bestPosKept = 0, bestNegRej = 0;

        for (float maxArea = 18.5f; maxArea <= 30f; maxArea += 0.5f) {
            for (float maxContrast = 0.70f; maxContrast <= 0.95f; maxContrast += 0.02f) {
                for (float midArea = 4f; midArea <= 14f; midArea += 1f) {
                    for (float midInside = 0.30f; midInside <= 0.75f; midInside += 0.05f) {
                        for (float hardInside = 0.05f; hardInside <= 0.30f; hardInside += 0.05f) {
                            int posKept = 0;
                            for (Blob b : pos) {
                                if (keep(b, maxArea, maxContrast, midArea, midInside, hardInside)) posKept++;
                            }
                            int negRej = 0;
                            for (Blob b : neg) {
                                if (!keep(b, maxArea, maxContrast, midArea, midInside, hardInside)) negRej++;
                            }
                            float posR = posKept / (float) pos.size();
                            float negR = negRej / (float) neg.size();
                            // Softer floor - we want max negative coverage
                            // even at the cost of a few positives.
                            if (posR < 0.88f) continue;
                            // Prefer higher negR; on ties, prefer higher posR;
                            // on ties, prefer wider tolerance (higher maxArea).
                            float score = negR * 100f + posR;
                            float bestScore = bestNegR * 100f + bestPosKept / (float) pos.size();
                            if (score > bestScore
                                    || (Math.abs(score - bestScore) < 1e-6f && maxArea > bMaxArea)) {
                                bestNegR = negR;
                                bMaxArea = maxArea;
                                bMaxContrast = maxContrast;
                                bMidArea = midArea;
                                bMidInside = midInside;
                                bHardInside = hardInside;
                                bestPosKept = posKept;
                                bestNegRej = negRej;
                            }
                        }
                    }
                }
            }
        }

        System.out.printf(Locale.ROOT,
                "BEST RULE: reject if%n"
                        + "    areaPct >= %.2f%n"
                        + " OR contrast >= %.3f%n"
                        + " OR (areaPct >= %.2f AND blobInsideAsphalt < %.2f)%n"
                        + " OR blobInsideAsphalt < %.2f%n",
                bMaxArea, bMaxContrast, bMidArea, bMidInside, bHardInside);
        System.out.printf(Locale.ROOT, "  positives kept    : %d / %d (%.1f%%)%n",
                bestPosKept, pos.size(), 100.0 * bestPosKept / pos.size());
        System.out.printf(Locale.ROOT, "  negatives rejected: %d / %d (%.1f%%)%n",
                bestNegRej, neg.size(), 100.0 * bestNegRej / neg.size());

        System.out.println("\nPer-category outcome:");
        for (var e : per.entrySet()) {
            int kept = 0;
            int total = e.getValue().size();
            for (Blob b : e.getValue()) {
                if (keep(b, bMaxArea, bMaxContrast, bMidArea, bMidInside, bHardInside)) kept++;
            }
            System.out.printf(Locale.ROOT, "  %-30s  %d / %d  (%s)%n",
                    e.getKey(), kept, total,
                    e.getKey().startsWith("POSITIVE") ? "want kept" : "want rejected");
        }

        System.out.println("\nPositives that the rule WOULD reject (false rejections):");
        for (Blob b : pos) {
            if (!keep(b, bMaxArea, bMaxContrast, bMidArea, bMidInside, bHardInside)) {
                System.out.printf(Locale.ROOT,
                        "  areaPct=%.2f  contrast=%.3f  insideAsphalt=%.2f  edgeTouches=%d%n",
                        b.areaPct, b.contrast, b.blobInsideAsphalt, b.edgeTouches);
            }
        }
    }

    static boolean keep(Blob b, float maxArea, float maxContrast,
                        float midArea, float midInside, float hardInside) {
        if (!b.detected) return true;       // no detection -> analyzer falls
                                            // back; not a rejection signal
        if (b.areaPct >= maxArea) return false;
        if (b.contrast >= maxContrast) return false;
        if (b.areaPct >= midArea && b.blobInsideAsphalt < midInside) return false;
        if (b.blobInsideAsphalt < hardInside) return false;
        return true;
    }

    static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
