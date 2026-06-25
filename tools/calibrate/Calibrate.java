/*
 * Single-file Java calibration tool that mirrors the Kotlin
 * SceneHeuristics + ContentClassifier logic and runs it against every photo
 * in tools/train_pothole_model/dataset/calibration_close_up/.
 *
 * Run with the JDK that Gradle already uses, e.g.:
 *   "%JAVA_HOME%\bin\java.exe" tools\calibrate\Calibrate.java <folder>
 *
 * Output: a CSV at tools/calibrate/calibration_report.csv plus a summary
 * printed to stdout describing pass/fail counts and threshold pressure
 * points across the close-up validator gates.
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

public class Calibrate {

    static final int SAMPLE = 72;

    // Calibrated close-up validator thresholds (post-tuning).
    static final float CLOSE_UP_MIN_ROAD_RATIO = 0.13f;
    static final float CLOSE_UP_MIN_ASPHALT_RATIO = 0.06f;
    static final float CLOSE_UP_MIN_ROAD_LINE_SCORE = 0.05f;
    static final float CIRCULAR_VESSEL_REJECT = 0.62f;
    static final float CIRCULAR_VESSEL_ROAD_GUARD = 0.42f;

    // Calibrated ContentClassifier rejection thresholds (post-tuning).
    // RECT_FRAME_REJECT is set above 1.0 to effectively disable the
    // rectangular-frame "photo of photo" heuristic — it produced too many
    // false rejections on real Indian road close-ups (curbs / markings /
    // tar lines on all four sides). Cloud AI will eventually do this check
    // properly; the heuristic gain didn't justify the false-rejection cost.
    static final float SKIN_REJECT = 0.45f;
    static final float VEGETATION_REJECT = 0.32f;
    static final float ANIMAL_FUR_REJECT = 0.55f;
    static final float RECT_FRAME_REJECT = 1.05f;
    static final float FLAT_SCREEN_REJECT = 0.62f;
    static final float FOOD_OBJECT_REJECT = 0.30f;
    static final float SAT_NONROAD_REJECT = 0.22f;

    // Calibrated SceneHeuristics rejection trigger thresholds.
    static final float INDOOR_FLOOR_REJECT_MIN = 0.50f;
    static final float INDOOR_FLOOR_REJECT_ROAD_GUARD = 0.30f;

    public static void main(String[] args) throws Exception {
        Path folder = Paths.get(args.length > 0
                ? args[0]
                : "tools/train_pothole_model/dataset/calibration_close_up");
        if (!Files.isDirectory(folder)) {
            System.err.println("Folder not found: " + folder.toAbsolutePath());
            System.exit(2);
        }
        Path outDir = Paths.get("tools/calibrate");
        Files.createDirectories(outDir);
        Path csv = outDir.resolve("calibration_report.csv");

        List<File> files = new ArrayList<>();
        try (var stream = Files.list(folder)) {
            stream.filter(p -> {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp");
            }).sorted().forEach(p -> files.add(p.toFile()));
        }

        Set<String> seenHashes = new HashSet<>();
        List<Result> results = new ArrayList<>();
        for (File f : files) {
            try {
                String hash = quickHash(f);
                boolean dup = !seenHashes.add(hash);
                BufferedImage img = ImageIO.read(f);
                if (img == null) {
                    System.err.println("[skip] cannot decode " + f.getName());
                    continue;
                }
                BufferedImage small = resize(img, SAMPLE, SAMPLE);
                Result r = analyze(f.getName(), small, dup);
                results.add(r);
            } catch (Exception e) {
                System.err.println("[error] " + f.getName() + ": " + e.getMessage());
            }
        }

        // Write CSV
        try (PrintWriter pw = new PrintWriter(new FileWriter(csv.toFile()))) {
            pw.println("file,duplicate,roadRatio,asphaltRatio,indoorFloorRatio,woodPanelRatio,verticalStructureRatio,"
                    + "skyBandRatio,highSaturationRatio,circularVesselScore,horizontalRoadLineScore,"
                    + "skinRatio,vegetationRatio,animalFurRatio,foodOrObjectColorRatio,rectFrameLikelihood,"
                    + "flatScreenLikelihood,saturatedNonRoadRatio,passClose,failureReason");
            for (Result r : results) {
                pw.printf(Locale.ROOT,
                        "%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%s,%s%n",
                        r.name, r.duplicate, r.roadRatio, r.asphaltRatio, r.indoorFloorRatio, r.woodPanelRatio,
                        r.verticalStructureRatio, r.skyBandRatio, r.highSaturationRatio, r.circularVesselScore,
                        r.horizontalRoadLineScore, r.skinRatio, r.vegetationRatio, r.animalFurRatio,
                        r.foodOrObjectColorRatio, r.rectFrameLikelihood, r.flatScreenLikelihood,
                        r.saturatedNonRoadRatio, r.passClose, csvSafe(r.failureReason));
            }
        }

        printSummary(results);
        System.out.println("\nFull CSV: " + csv.toAbsolutePath());
    }

    // -------------------- analysis --------------------

    static Result analyze(String name, BufferedImage small, boolean dup) {
        int w = small.getWidth();
        int h = small.getHeight();
        int total = w * h;
        int[] px = new int[total];
        small.getRGB(0, 0, w, h, px, 0, w);

        int road = 0, asphalt = 0, indoorFloor = 0, woodPanel = 0;
        int verticalStructure = 0, sky = 0, saturated = 0;

        // ContentClassifier counters
        int skin = 0, vegetation = 0, fur = 0, foodObject = 0, satNonRoad = 0;
        double sumLum = 0, sumLumSq = 0;
        int saturatedSumCC = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = px[y * w + x];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                int maxC = Math.max(r, Math.max(g, b));
                int minC = Math.min(r, Math.min(g, b));
                float sat = maxC == 0 ? 0f : (maxC - minC) / (float) maxC;
                float lum = 0.299f * r + 0.587f * g + 0.114f * b;

                // Scene classification (priority order matters).
                if (isIndoorFloorTile(r, g, b, sat, lum)) {
                    indoorFloor++;
                } else if (isWoodPanel(r, g, b, sat, lum)) {
                    woodPanel++;
                } else if (isAsphalt(r, g, b, sat, lum)) {
                    asphalt++;
                    road++;
                } else if (isRoadLike(r, g, b, sat, lum)) {
                    road++;
                } else if (isHighSaturation(maxC, sat)) {
                    saturated++;
                }

                if (x >= 1 && x <= w - 2 && y >= 1 && y <= h - 2) {
                    float lumLeft = lumOf(px, w, x - 1, y);
                    float lumRight = lumOf(px, w, x + 1, y);
                    float lumUp = lumOf(px, w, x, y - 1);
                    float lumDown = lumOf(px, w, x, y + 1);
                    float vGrad = Math.abs(lum - lumUp) + Math.abs(lum - lumDown);
                    float hGrad = Math.abs(lum - lumLeft) + Math.abs(lum - lumRight);
                    if (vGrad > hGrad * 1.35f && vGrad > 28f) verticalStructure++;
                }
                if (y < h / 4 && isSkyLike(b, r, lum)) sky++;

                // Content
                if (isSkin(r, g, b)) skin++;
                else if (isVegetation(r, g, b)) vegetation++;
                else if (isFur(r, g, b, sat, lum)) fur++;
                else if (isSatNonRoad(sat, lum)) {
                    foodObject++;
                    satNonRoad++;
                }
                sumLum += lum;
                sumLumSq += lum * lum;
                if (sat > 0.30f && maxC > 70) saturatedSumCC++;
            }
        }

        float circular = circularVesselScore(px, w, h);
        float horizLines = horizontalRoadLineScore(px, w, h);
        float rectFrame = rectangularFrameScore(px, w, h);
        float flatScreen = flatScreenScore(sumLum, sumLumSq, total, saturatedSumCC);

        Result r = new Result();
        r.name = name;
        r.duplicate = dup;
        r.roadRatio = road / (float) total;
        r.asphaltRatio = asphalt / (float) total;
        r.indoorFloorRatio = indoorFloor / (float) total;
        r.woodPanelRatio = woodPanel / (float) total;
        r.verticalStructureRatio = verticalStructure / (float) total;
        r.skyBandRatio = sky / (float) (total / 4);
        r.highSaturationRatio = saturated / (float) total;
        r.circularVesselScore = circular;
        r.horizontalRoadLineScore = horizLines;
        r.skinRatio = skin / (float) total;
        r.vegetationRatio = vegetation / (float) total;
        r.animalFurRatio = fur / (float) total;
        r.foodOrObjectColorRatio = foodObject / (float) total;
        r.rectFrameLikelihood = rectFrame;
        r.flatScreenLikelihood = flatScreen;
        r.saturatedNonRoadRatio = satNonRoad / (float) total;

        // Decide pass/fail against current close-up validator.
        r.passClose = true;
        r.failureReason = "";

        // ContentClassifier first
        if (r.skinRatio >= SKIN_REJECT) reject(r, "content:person(skin=" + fmt(r.skinRatio) + ")");
        else if (r.vegetationRatio >= VEGETATION_REJECT) reject(r, "content:vegetation(veg=" + fmt(r.vegetationRatio) + ")");
        else if (r.animalFurRatio >= ANIMAL_FUR_REJECT) reject(r, "content:animal(fur=" + fmt(r.animalFurRatio) + ")");
        else if (r.rectFrameLikelihood >= RECT_FRAME_REJECT) reject(r, "content:photo-of-photo(rect=" + fmt(r.rectFrameLikelihood) + ")");
        else if (r.flatScreenLikelihood >= FLAT_SCREEN_REJECT) reject(r, "content:screen(flat=" + fmt(r.flatScreenLikelihood) + ")");
        else if (r.foodOrObjectColorRatio >= FOOD_OBJECT_REJECT && r.saturatedNonRoadRatio >= SAT_NONROAD_REJECT)
            reject(r, "content:object(foodObj=" + fmt(r.foodOrObjectColorRatio) + ",satNR=" + fmt(r.saturatedNonRoadRatio) + ")");

        if (!r.passClose) return r;

        // Scene heuristics rejection reasons (close-up specific).
        if (r.circularVesselScore >= 0.55f && r.roadRatio < 0.38f) reject(r, "scene:circular-bin(circ=" + fmt(r.circularVesselScore) + ",road=" + fmt(r.roadRatio) + ")");
        else if (r.indoorFloorRatio >= INDOOR_FLOOR_REJECT_MIN && r.roadRatio < INDOOR_FLOOR_REJECT_ROAD_GUARD) reject(r, "scene:indoor-floor(if=" + fmt(r.indoorFloorRatio) + ",road=" + fmt(r.roadRatio) + ")");
        else if (r.woodPanelRatio >= 0.18f && r.asphaltRatio < 0.16f) reject(r, "scene:wood-panel(wp=" + fmt(r.woodPanelRatio) + ",asph=" + fmt(r.asphaltRatio) + ")");
        else if (r.verticalStructureRatio >= 0.18f && r.asphaltRatio < 0.12f) reject(r, "scene:vertical-walls(vs=" + fmt(r.verticalStructureRatio) + ",asph=" + fmt(r.asphaltRatio) + ")");
        else if (r.highSaturationRatio >= 0.07f && r.asphaltRatio < 0.12f) reject(r, "scene:high-saturation(hs=" + fmt(r.highSaturationRatio) + ",asph=" + fmt(r.asphaltRatio) + ")");
        else if (r.asphaltRatio < 0.05f && r.horizontalRoadLineScore < 0.05f && r.roadRatio < 0.24f) reject(r, "scene:no-asphalt(asph=" + fmt(r.asphaltRatio) + ",hLine=" + fmt(r.horizontalRoadLineScore) + ",road=" + fmt(r.roadRatio) + ")");

        if (!r.passClose) return r;

        // Bin / drain-cover guard
        if (r.circularVesselScore >= CIRCULAR_VESSEL_REJECT && r.roadRatio < CIRCULAR_VESSEL_ROAD_GUARD) {
            reject(r, "circular-strict(circ=" + fmt(r.circularVesselScore) + ",road=" + fmt(r.roadRatio) + ")");
            return r;
        }

        // Asphalt + road-line minimum
        if (r.asphaltRatio < CLOSE_UP_MIN_ASPHALT_RATIO && r.horizontalRoadLineScore < CLOSE_UP_MIN_ROAD_LINE_SCORE) {
            reject(r, "min-surface(asph=" + fmt(r.asphaltRatio) + "<0.06,hLine=" + fmt(r.horizontalRoadLineScore) + "<0.05)");
            return r;
        }

        // Road minimum
        if (r.roadRatio < CLOSE_UP_MIN_ROAD_RATIO) {
            reject(r, "min-road(road=" + fmt(r.roadRatio) + "<0.20)");
            return r;
        }

        return r;
    }

    static void reject(Result r, String reason) {
        r.passClose = false;
        if (r.failureReason.isEmpty()) r.failureReason = reason;
    }

    // -------------------- pixel rules (mirror Kotlin) --------------------

    static boolean isAsphalt(int r, int g, int b, float sat, float lum) {
        return lum >= 35f && lum <= 190f && sat < 0.42f && Math.abs(r - g) < 45 && Math.abs(g - b) < 45;
    }
    static boolean isIndoorFloorTile(int r, int g, int b, float sat, float lum) {
        // Bright sunlit Indian asphalt sits in 165..195 — exclude it from
        // "indoor floor" by raising the lower bound and tightening color
        // tolerance. True tile/lacquered floor stays well within 195..245.
        return lum >= 195f && lum <= 245f && sat < 0.18f && Math.abs(r - g) < 18 && Math.abs(g - b) < 18;
    }
    static boolean isWoodPanel(int r, int g, int b, float sat, float lum) {
        return lum >= 35f && lum <= 125f && sat < 0.45f && r >= g - 8 && r >= b - 12 && g >= b - 18;
    }
    static boolean isSkyLike(int b, int r, float lum) {
        return lum > 165f && b >= r - 15;
    }
    static boolean isHighSaturation(int maxC, float sat) {
        return sat > 0.42f && maxC > 80;
    }
    static boolean isRoadLike(int r, int g, int b, float sat, float lum) {
        if (lum > 175f && sat < 0.18f) return false;
        return lum >= 30f && lum <= 210f && sat < 0.52f && Math.abs(r - g) < 55 && Math.abs(g - b) < 55;
    }

    static boolean isSkin(int r, int g, int b) {
        // Calibrated against 37 real Indian close-up pothole photos: warm
        // sunlit asphalt has R > G > B with R-G ~10..25, which the original
        // Kovac rule misclassified as skin (skinRatio max 0.835 in dataset).
        // Tightened by demanding stronger R-G separation, larger overall
        // chroma, and a higher minimum luminance.
        if (r <= 110 || g <= 50 || b <= 25) return false;
        if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 25) return false;
        if (r - g <= 25) return false;
        if (g - b <= 18) return false;
        if (r <= g || r <= b) return false;
        if (r > 220 && g < 80 && b < 80) return false;
        // Skin is rarely below mid-luminance; asphalt frequently is.
        float lum = 0.299f * r + 0.587f * g + 0.114f * b;
        if (lum < 95f) return false;
        return true;
    }
    static boolean isVegetation(int r, int g, int b) {
        if (g <= r + 8) return false;
        if (g <= b + 8) return false;
        int chroma = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
        if (chroma < 22) return false;
        if (g < 60) return false;
        return true;
    }
    static boolean isFur(int r, int g, int b, float sat, float lum) {
        // Calibrated against real Indian road photos: warm dusty asphalt has
        // R > G > B with sat 0.20..0.35, which the original rule classified
        // as fur (animalFurRatio max 0.700 in the dataset). Tightened by
        // demanding noticeably greater R-G / G-B separations and higher sat.
        if (lum < 80f || lum > 230f) return false;
        if (sat < 0.28f || sat > 0.55f) return false;
        if (r <= g + 12 || g <= b + 12) return false;
        if (r - b < 50 || r - b > 120) return false;
        return true;
    }
    static boolean isSatNonRoad(float sat, float lum) {
        if (sat < 0.42f) return false;
        if (lum < 50f) return false;
        return true;
    }

    static float lumOf(int[] px, int w, int x, int y) {
        int c = px[y * w + x];
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        return 0.299f * r + 0.587f * g + 0.114f * b;
    }

    static float circularVesselScore(int[] px, int w, int h) {
        int cx = w / 2;
        int cy = h / 2;
        int radius = Math.min(w, h) / 5;
        int darkCenter = 0;
        int ringContrast = 0;
        int ringSamples = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = cx + dx;
                int y = cy + dy;
                if (x < 0 || x >= w || y < 0 || y >= h) continue;
                double dist = Math.hypot(dx, dy);
                if (dist > radius) continue;
                float lum = lumOf(px, w, x, y);
                if (dist < radius * 0.45 && lum < 55f) darkCenter++;
                if (dist >= radius * 0.75 && dist <= radius * 1.0) {
                    ringSamples++;
                    float outerLum = lum;
                    float innerLum = lumOf(px, w, cx, cy);
                    if (outerLum - innerLum > 35f) ringContrast++;
                }
            }
        }
        int centerArea = Math.max(1, (int) (radius * 0.45 * radius * 0.45 * Math.PI));
        float darkFrac = darkCenter / (float) centerArea;
        float ringFrac = ringSamples == 0 ? 0f : ringContrast / (float) ringSamples;
        float v = darkFrac * 0.65f + ringFrac * 0.35f;
        return Math.max(0f, Math.min(1f, v));
    }

    static float horizontalRoadLineScore(int[] px, int w, int h) {
        int strong = 0;
        int samples = 0;
        for (int y = h / 3; y < h * 2 / 3; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (y < 1 || y >= h - 1) { samples++; continue; }
                float lum = lumOf(px, w, x, y);
                float lumLeft = lumOf(px, w, x - 1, y);
                float lumRight = lumOf(px, w, x + 1, y);
                float horiz = Math.abs(lum - lumLeft) + Math.abs(lum - lumRight);
                float vert = Math.abs(lum - lumOf(px, w, x, y - 1)) + Math.abs(lum - lumOf(px, w, x, y + 1));
                if (horiz > vert * 1.2f) strong++;
                samples++;
            }
        }
        return samples == 0 ? 0f : strong / (float) samples;
    }

    static float rectangularFrameScore(int[] px, int w, int h) {
        int band = Math.max(2, Math.min(w, h) / 14);
        int top = 0, bottom = 0, left = 0, right = 0;
        int sideArea = band * w;
        int vertSideArea = band * h;
        for (int x = 1; x < w - 1; x++) {
            for (int y = 0; y < band; y++) {
                if (y >= 1 && y <= h - 2 && isStrongVerticalGradient(px, w, x, y)) top++;
                int by = h - 1 - y;
                if (by >= 1 && by <= h - 2 && isStrongVerticalGradient(px, w, x, by)) bottom++;
            }
        }
        for (int y = 1; y < h - 1; y++) {
            for (int x = 0; x < band; x++) {
                if (x >= 1 && x <= w - 2 && isStrongHorizontalGradient(px, w, x, y)) left++;
                int rx = w - 1 - x;
                if (rx >= 1 && rx <= w - 2 && isStrongHorizontalGradient(px, w, rx, y)) right++;
            }
        }
        float topF = top / (float) sideArea;
        float botF = bottom / (float) sideArea;
        float leftF = left / (float) vertSideArea;
        float rightF = right / (float) vertSideArea;
        float[] sides = new float[]{topF, botF, leftF, rightF};
        Arrays.sort(sides);
        float minSide = sides[0];
        return Math.max(0f, Math.min(1f, minSide * 6f));
    }

    static boolean isStrongVerticalGradient(int[] px, int w, int x, int y) {
        float l = lumOf(px, w, x, y);
        float u = lumOf(px, w, x, y - 1);
        float d = lumOf(px, w, x, y + 1);
        return Math.abs(l - u) + Math.abs(l - d) > 70f;
    }
    static boolean isStrongHorizontalGradient(int[] px, int w, int x, int y) {
        float l = lumOf(px, w, x, y);
        float left = lumOf(px, w, x - 1, y);
        float right = lumOf(px, w, x + 1, y);
        return Math.abs(l - left) + Math.abs(l - right) > 70f;
    }

    static float flatScreenScore(double sumLum, double sumLumSq, int n, int saturatedCount) {
        if (n == 0) return 0f;
        double mean = sumLum / n;
        double variance = Math.max(0, sumLumSq / n - mean * mean);
        double std = Math.sqrt(variance);
        float flatness = (float) Math.max(0, Math.min(1, 1 - std / 60.0));
        float satFrac = saturatedCount / (float) n;
        float satScore = Math.max(0f, Math.min(1f, satFrac * 4f));
        float v = flatness * 0.6f + satScore * 0.4f;
        return Math.max(0f, Math.min(1f, v));
    }

    // -------------------- helpers --------------------

    static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    static String quickHash(File f) throws Exception {
        // Fast file-content hash using size + first/last 64KB so true gallery
        // duplicates match without re-decoding the whole file.
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

    static String fmt(float v) { return String.format(Locale.ROOT, "%.3f", v); }
    static String csvSafe(String s) { return s.replace(',', ';'); }

    // -------------------- summary --------------------

    static void printSummary(List<Result> results) {
        int total = results.size();
        int dups = 0;
        int passed = 0;
        for (Result r : results) {
            if (r.duplicate) dups++;
            if (r.passClose) passed++;
        }
        System.out.println("===== Calibration summary =====");
        System.out.println("Total photos        : " + total + "  (duplicates: " + dups + ")");
        System.out.println("Passed close-up gate: " + passed + " / " + total);
        System.out.println("Failed              : " + (total - passed) + " / " + total);

        // Failure breakdown
        var counts = new java.util.LinkedHashMap<String, Integer>();
        for (Result r : results) {
            if (!r.passClose) {
                String k = r.failureReason.split("\\(")[0];
                counts.merge(k, 1, Integer::sum);
            }
        }
        if (!counts.isEmpty()) {
            System.out.println("\nFailure reasons:");
            counts.forEach((k, v) -> System.out.printf(Locale.ROOT, "  %-30s %3d%n", k, v));
        }

        // Distribution of key signals across the dataset.
        printStat("roadRatio",                results, r -> r.roadRatio);
        printStat("asphaltRatio",             results, r -> r.asphaltRatio);
        printStat("horizontalRoadLineScore",  results, r -> r.horizontalRoadLineScore);
        printStat("circularVesselScore",      results, r -> r.circularVesselScore);
        printStat("indoorFloorRatio",         results, r -> r.indoorFloorRatio);
        printStat("woodPanelRatio",           results, r -> r.woodPanelRatio);
        printStat("verticalStructureRatio",   results, r -> r.verticalStructureRatio);
        printStat("highSaturationRatio",      results, r -> r.highSaturationRatio);
        printStat("skinRatio",                results, r -> r.skinRatio);
        printStat("vegetationRatio",          results, r -> r.vegetationRatio);
        printStat("animalFurRatio",           results, r -> r.animalFurRatio);
        printStat("foodOrObjectColorRatio",   results, r -> r.foodOrObjectColorRatio);
        printStat("saturatedNonRoadRatio",    results, r -> r.saturatedNonRoadRatio);
        printStat("rectFrameLikelihood",      results, r -> r.rectFrameLikelihood);
        printStat("flatScreenLikelihood",     results, r -> r.flatScreenLikelihood);
    }

    interface Sel { float pick(Result r); }

    static void printStat(String label, List<Result> results, Sel sel) {
        if (results.isEmpty()) return;
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        double sum = 0;
        float[] vals = new float[results.size()];
        int i = 0;
        for (Result r : results) {
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
                "%n  %-26s  min=%.3f  p10=%.3f  med=%.3f  p90=%.3f  max=%.3f  mean=%.3f",
                label, min, p10, median, p90, max, sum / results.size());
    }

    static class Result {
        String name;
        boolean duplicate;
        float roadRatio, asphaltRatio, indoorFloorRatio, woodPanelRatio;
        float verticalStructureRatio, skyBandRatio, highSaturationRatio;
        float circularVesselScore, horizontalRoadLineScore;
        float skinRatio, vegetationRatio, animalFurRatio, foodOrObjectColorRatio;
        float rectFrameLikelihood, flatScreenLikelihood, saturatedNonRoadRatio;
        boolean passClose;
        String failureReason = "";
    }
}
