/*
 * Run the close-up validator (current Kotlin SceneHeuristics +
 * ContentClassifier rules) against the Pothole Negative samples folder
 * subfolders. Each subfolder is a category. For each category we report:
 *
 *   - false-positive rate   = how many photos the validator currently
 *                             ACCEPTS that should be rejected
 *   - per-signal distribution (so we can design new rejection rules
 *     without touching the 37 positive close-ups)
 *
 * Run with:
 *   "%JAVA_HOME%\bin\java.exe" tools\calibrate\CalibrateNegatives.java
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalibrateNegatives {

    static final int SAMPLE = 72;

    // Current Kotlin thresholds (post-positive-calibration round).
    static final float CLOSE_UP_MIN_ROAD_RATIO = 0.13f;
    static final float CLOSE_UP_MIN_ASPHALT_RATIO = 0.06f;
    static final float CLOSE_UP_MIN_ROAD_LINE_SCORE = 0.05f;
    static final float CIRCULAR_VESSEL_REJECT = 0.62f;
    static final float CIRCULAR_VESSEL_ROAD_GUARD = 0.42f;
    static final float SKIN_REJECT = 0.45f;
    static final float VEGETATION_REJECT = 0.32f;
    static final float ANIMAL_FUR_REJECT = 0.55f;
    static final float RECT_FRAME_REJECT = 1.05f;
    static final float FLAT_SCREEN_REJECT = 0.62f;
    static final float FOOD_OBJECT_REJECT = 0.30f;
    static final float SAT_NONROAD_REJECT = 0.22f;
    static final float INDOOR_FLOOR_REJECT_MIN = 0.50f;
    static final float INDOOR_FLOOR_REJECT_ROAD_GUARD = 0.30f;

    public static void main(String[] args) throws Exception {
        Path root = Paths.get("tools/train_pothole_model/dataset/Pothole Negative samples");
        if (!Files.isDirectory(root)) {
            System.err.println("Negatives root not found: " + root.toAbsolutePath());
            System.exit(2);
        }
        Path outDir = Paths.get("tools/calibrate");
        Files.createDirectories(outDir);
        Path csv = outDir.resolve("negatives_report.csv");

        List<Path> categories = new ArrayList<>();
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory).sorted().forEach(categories::add);
        }

        Map<String, List<Result>> all = new LinkedHashMap<>();
        for (Path cat : categories) {
            String name = cat.getFileName().toString();
            List<Result> results = new ArrayList<>();
            try (var stream = Files.list(cat)) {
                List<File> imgs = new ArrayList<>();
                stream.filter(p -> {
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return n.endsWith(".jpg") || n.endsWith(".jpeg")
                            || n.endsWith(".png") || n.endsWith(".webp");
                }).sorted().forEach(p -> imgs.add(p.toFile()));
                for (File f : imgs) {
                    BufferedImage img = ImageIO.read(f);
                    if (img == null) continue;
                    BufferedImage small = resize(img, SAMPLE, SAMPLE);
                    Result r = analyze(name, f.getName(), small);
                    results.add(r);
                }
            }
            all.put(name, results);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(csv.toFile()))) {
            pw.println("category,file,roadRatio,asphaltRatio,indoorFloorRatio,woodPanelRatio,verticalStructureRatio,"
                    + "skyBandRatio,highSaturationRatio,circularVesselScore,horizontalRoadLineScore,"
                    + "skinRatio,vegetationRatio,animalFurRatio,foodOrObjectColorRatio,rectFrameLikelihood,"
                    + "flatScreenLikelihood,saturatedNonRoadRatio,brightCircleRingScore,uniformWallScore,leafyTextureScore,"
                    + "passClose,failureReason");
            for (var e : all.entrySet()) {
                for (Result r : e.getValue()) {
                    pw.printf(Locale.ROOT,
                            "%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%s,%s%n",
                            e.getKey(), r.name,
                            r.roadRatio, r.asphaltRatio, r.indoorFloorRatio, r.woodPanelRatio,
                            r.verticalStructureRatio, r.skyBandRatio, r.highSaturationRatio,
                            r.circularVesselScore, r.horizontalRoadLineScore,
                            r.skinRatio, r.vegetationRatio, r.animalFurRatio, r.foodOrObjectColorRatio,
                            r.rectFrameLikelihood, r.flatScreenLikelihood, r.saturatedNonRoadRatio,
                            r.brightCircleRingScore, r.uniformWallScore, r.leafyTextureScore,
                            r.passClose, csvSafe(r.failureReason));
                }
            }
        }

        printPerCategorySummary(all);
        System.out.println("\nFull CSV: " + csv.toAbsolutePath());
    }

    // -------------------- analysis --------------------

    static Result analyze(String category, String name, BufferedImage small) {
        int w = small.getWidth();
        int h = small.getHeight();
        int total = w * h;
        int[] px = new int[total];
        small.getRGB(0, 0, w, h, px, 0, w);

        int road = 0, asphalt = 0, indoorFloor = 0, woodPanel = 0;
        int verticalStructure = 0, sky = 0, saturated = 0;
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

                if (isIndoorFloorTile(r, g, b, sat, lum)) indoorFloor++;
                else if (isWoodPanel(r, g, b, sat, lum)) woodPanel++;
                else if (isAsphalt(r, g, b, sat, lum)) { asphalt++; road++; }
                else if (isRoadLike(r, g, b, sat, lum)) road++;
                else if (isHighSaturation(maxC, sat)) saturated++;

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

                if (isSkin(r, g, b)) skin++;
                else if (isVegetation(r, g, b)) vegetation++;
                else if (isFur(r, g, b, sat, lum)) fur++;
                else if (isSatNonRoad(sat, lum)) { foodObject++; satNonRoad++; }

                sumLum += lum;
                sumLumSq += lum * lum;
                if (sat > 0.30f && maxC > 70) saturatedSumCC++;
            }
        }

        Result r = new Result();
        r.category = category;
        r.name = name;
        r.roadRatio = road / (float) total;
        r.asphaltRatio = asphalt / (float) total;
        r.indoorFloorRatio = indoorFloor / (float) total;
        r.woodPanelRatio = woodPanel / (float) total;
        r.verticalStructureRatio = verticalStructure / (float) total;
        r.skyBandRatio = sky / (float) (total / 4);
        r.highSaturationRatio = saturated / (float) total;
        r.circularVesselScore = circularVesselScore(px, w, h);
        r.horizontalRoadLineScore = horizontalRoadLineScore(px, w, h);
        r.skinRatio = skin / (float) total;
        r.vegetationRatio = vegetation / (float) total;
        r.animalFurRatio = fur / (float) total;
        r.foodOrObjectColorRatio = foodObject / (float) total;
        r.rectFrameLikelihood = rectangularFrameScore(px, w, h);
        r.flatScreenLikelihood = flatScreenScore(sumLum, sumLumSq, total, saturatedSumCC);
        r.saturatedNonRoadRatio = satNonRoad / (float) total;

        // NEW PROPOSED SIGNALS for negative-category detection (not yet used
        // for rejection; we measure them to design rules).
        r.brightCircleRingScore = brightCircleRingScore(px, w, h);
        r.uniformWallScore = uniformWallScore(px, w, h, sumLum, sumLumSq, total);
        r.leafyTextureScore = leafyTextureScore(px, w, h);

        // Run the current rejection logic.
        r.passClose = true;
        if (r.skinRatio >= SKIN_REJECT) reject(r, "content:person");
        else if (r.vegetationRatio >= VEGETATION_REJECT) reject(r, "content:vegetation");
        else if (r.animalFurRatio >= ANIMAL_FUR_REJECT) reject(r, "content:animal");
        else if (r.rectFrameLikelihood >= RECT_FRAME_REJECT) reject(r, "content:photo-of-photo");
        else if (r.flatScreenLikelihood >= FLAT_SCREEN_REJECT) reject(r, "content:screen");
        else if (r.foodOrObjectColorRatio >= FOOD_OBJECT_REJECT && r.saturatedNonRoadRatio >= SAT_NONROAD_REJECT)
            reject(r, "content:object");
        if (!r.passClose) return r;

        if (r.circularVesselScore >= 0.55f && r.roadRatio < 0.38f) reject(r, "scene:circular-bin");
        else if (r.indoorFloorRatio >= INDOOR_FLOOR_REJECT_MIN && r.roadRatio < INDOOR_FLOOR_REJECT_ROAD_GUARD)
            reject(r, "scene:indoor-floor");
        else if (r.woodPanelRatio >= 0.18f && r.asphaltRatio < 0.16f) reject(r, "scene:wood-panel");
        else if (r.verticalStructureRatio >= 0.18f && r.asphaltRatio < 0.12f) reject(r, "scene:vertical-walls");
        else if (r.highSaturationRatio >= 0.07f && r.asphaltRatio < 0.12f) reject(r, "scene:high-saturation");
        else if (r.asphaltRatio < 0.05f && r.horizontalRoadLineScore < 0.05f && r.roadRatio < 0.24f)
            reject(r, "scene:no-asphalt");
        if (!r.passClose) return r;

        if (r.circularVesselScore >= CIRCULAR_VESSEL_REJECT && r.roadRatio < CIRCULAR_VESSEL_ROAD_GUARD)
            reject(r, "circular-strict");
        if (!r.passClose) return r;

        if (r.asphaltRatio < CLOSE_UP_MIN_ASPHALT_RATIO && r.horizontalRoadLineScore < CLOSE_UP_MIN_ROAD_LINE_SCORE)
            reject(r, "min-surface");
        if (!r.passClose) return r;

        if (r.roadRatio < CLOSE_UP_MIN_ROAD_RATIO) reject(r, "min-road");
        return r;
    }

    static void reject(Result r, String reason) {
        r.passClose = false;
        if (r.failureReason.isEmpty()) r.failureReason = reason;
    }

    // ===================== pixel rules =====================

    static boolean isAsphalt(int r, int g, int b, float sat, float lum) {
        return lum >= 35f && lum <= 190f && sat < 0.42f && Math.abs(r - g) < 45 && Math.abs(g - b) < 45;
    }
    static boolean isIndoorFloorTile(int r, int g, int b, float sat, float lum) {
        return lum >= 195f && lum <= 245f && sat < 0.18f && Math.abs(r - g) < 18 && Math.abs(g - b) < 18;
    }
    static boolean isWoodPanel(int r, int g, int b, float sat, float lum) {
        return lum >= 35f && lum <= 125f && sat < 0.45f && r >= g - 8 && r >= b - 12 && g >= b - 18;
    }
    static boolean isSkyLike(int b, int r, float lum) { return lum > 165f && b >= r - 15; }
    static boolean isHighSaturation(int maxC, float sat) { return sat > 0.42f && maxC > 80; }
    static boolean isRoadLike(int r, int g, int b, float sat, float lum) {
        if (lum > 175f && sat < 0.18f) return false;
        return lum >= 30f && lum <= 210f && sat < 0.52f && Math.abs(r - g) < 55 && Math.abs(g - b) < 55;
    }
    static boolean isSkin(int r, int g, int b) {
        if (r <= 110 || g <= 50 || b <= 25) return false;
        if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 25) return false;
        if (r - g <= 25) return false;
        if (g - b <= 18) return false;
        if (r <= g || r <= b) return false;
        if (r > 220 && g < 80 && b < 80) return false;
        float lum = 0.299f * r + 0.587f * g + 0.114f * b;
        return lum >= 95f;
    }
    static boolean isVegetation(int r, int g, int b) {
        if (g <= r + 8 || g <= b + 8) return false;
        int chroma = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
        return chroma >= 22 && g >= 60;
    }
    static boolean isFur(int r, int g, int b, float sat, float lum) {
        if (lum < 80f || lum > 230f) return false;
        if (sat < 0.28f || sat > 0.55f) return false;
        if (r <= g + 12 || g <= b + 12) return false;
        return r - b >= 50 && r - b <= 120;
    }
    static boolean isSatNonRoad(float sat, float lum) { return sat >= 0.42f && lum >= 50f; }

    static float lumOf(int[] px, int w, int x, int y) {
        int c = px[y * w + x];
        return 0.299f * ((c >> 16) & 0xFF) + 0.587f * ((c >> 8) & 0xFF) + 0.114f * (c & 0xFF);
    }

    // ===================== existing scores =====================

    static float circularVesselScore(int[] px, int w, int h) {
        int cx = w / 2, cy = h / 2;
        int radius = Math.min(w, h) / 5;
        int darkCenter = 0, ringContrast = 0, ringSamples = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = cx + dx, y = cy + dy;
                if (x < 0 || x >= w || y < 0 || y >= h) continue;
                double dist = Math.hypot(dx, dy);
                if (dist > radius) continue;
                float lum = lumOf(px, w, x, y);
                if (dist < radius * 0.45 && lum < 55f) darkCenter++;
                if (dist >= radius * 0.75 && dist <= radius) {
                    ringSamples++;
                    if (lum - lumOf(px, w, cx, cy) > 35f) ringContrast++;
                }
            }
        }
        int centerArea = Math.max(1, (int) (radius * 0.45 * radius * 0.45 * Math.PI));
        float darkFrac = darkCenter / (float) centerArea;
        float ringFrac = ringSamples == 0 ? 0f : ringContrast / (float) ringSamples;
        return clamp01(darkFrac * 0.65f + ringFrac * 0.35f);
    }

    static float horizontalRoadLineScore(int[] px, int w, int h) {
        int strong = 0, samples = 0;
        for (int y = h / 3; y < h * 2 / 3; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (y < 1 || y >= h - 1) { samples++; continue; }
                float lum = lumOf(px, w, x, y);
                float horiz = Math.abs(lum - lumOf(px, w, x - 1, y)) + Math.abs(lum - lumOf(px, w, x + 1, y));
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
                if (y >= 1 && y <= h - 2 && grad(px, w, x, y, true) > 70f) top++;
                int by = h - 1 - y;
                if (by >= 1 && by <= h - 2 && grad(px, w, x, by, true) > 70f) bottom++;
            }
        }
        for (int y = 1; y < h - 1; y++) {
            for (int x = 0; x < band; x++) {
                if (x >= 1 && x <= w - 2 && grad(px, w, x, y, false) > 70f) left++;
                int rx = w - 1 - x;
                if (rx >= 1 && rx <= w - 2 && grad(px, w, rx, y, false) > 70f) right++;
            }
        }
        float[] sides = new float[]{top / (float) sideArea, bottom / (float) sideArea,
                left / (float) vertSideArea, right / (float) vertSideArea};
        Arrays.sort(sides);
        return clamp01(sides[0] * 6f);
    }

    static float grad(int[] px, int w, int x, int y, boolean vertical) {
        float l = lumOf(px, w, x, y);
        if (vertical) return Math.abs(l - lumOf(px, w, x, y - 1)) + Math.abs(l - lumOf(px, w, x, y + 1));
        return Math.abs(l - lumOf(px, w, x - 1, y)) + Math.abs(l - lumOf(px, w, x + 1, y));
    }

    static float flatScreenScore(double sumLum, double sumLumSq, int n, int satCount) {
        if (n == 0) return 0f;
        double mean = sumLum / n;
        double variance = Math.max(0, sumLumSq / n - mean * mean);
        double std = Math.sqrt(variance);
        float flatness = (float) clamp01(1 - std / 60.0);
        float satScore = clamp01(satCount / (float) n * 4f);
        return clamp01(flatness * 0.6f + satScore * 0.4f);
    }

    // ===================== NEW PROPOSED SIGNALS =====================

    /**
     * Bright circular ring — typical signature of cast-iron manhole covers
     * (the cover itself is brighter than the surrounding asphalt and bordered
     * by a clear circular gap). Inverse polarity of circularVesselScore.
     */
    static float brightCircleRingScore(int[] px, int w, int h) {
        int cx = w / 2, cy = h / 2;
        int radius = Math.min(w, h) / 3;
        float center = lumOf(px, w, cx, cy);
        // Sample center 0.45R - need brightness; ring at 0.85R - need darkness
        int brightCenter = 0, centerN = 0;
        int darkRing = 0, ringN = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = cx + dx, y = cy + dy;
                if (x < 0 || x >= w || y < 0 || y >= h) continue;
                double dist = Math.hypot(dx, dy);
                if (dist > radius) continue;
                float lum = lumOf(px, w, x, y);
                if (dist < radius * 0.4) {
                    centerN++;
                    if (lum > 80f) brightCenter++;
                }
                if (dist >= radius * 0.85 && dist <= radius) {
                    ringN++;
                    if (center - lum > 25f || (lum < 60f)) darkRing++;
                }
            }
        }
        if (centerN == 0 || ringN == 0) return 0f;
        float bright = brightCenter / (float) centerN;
        float dark = darkRing / (float) ringN;
        return clamp01(bright * 0.55f + dark * 0.45f);
    }

    /**
     * Uniform smooth wall — overall low texture variance, consistent colour,
     * low road-line score, and asphalt is not the dominant surface. Painted
     * walls / cement pipelines / sign boards stand out here.
     */
    static float uniformWallScore(int[] px, int w, int h, double sumLum, double sumLumSq, int n) {
        // Global luminance flatness.
        double mean = sumLum / n;
        double variance = Math.max(0, sumLumSq / n - mean * mean);
        double std = Math.sqrt(variance);
        float flatness = (float) clamp01(1 - std / 50.0);
        // Local-gradient sparsity in middle band.
        int strong = 0, samples = 0;
        for (int y = h / 4; y < h * 3 / 4; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (y < 1 || y >= h - 1) continue;
                float l = lumOf(px, w, x, y);
                float total = Math.abs(l - lumOf(px, w, x - 1, y)) + Math.abs(l - lumOf(px, w, x + 1, y))
                        + Math.abs(l - lumOf(px, w, x, y - 1)) + Math.abs(l - lumOf(px, w, x, y + 1));
                if (total > 35f) strong++;
                samples++;
            }
        }
        float gradFrac = samples == 0 ? 0f : strong / (float) samples;
        float gradLow = clamp01(1f - gradFrac * 4f);
        return clamp01(flatness * 0.55f + gradLow * 0.45f);
    }

    /**
     * Leafy / busy texture - random high-frequency micro-edges with
     * non-grey color. Trees and dry shrubs.
     */
    static float leafyTextureScore(int[] px, int w, int h) {
        int chroma = 0, microEdges = 0;
        int total = 0;
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                total++;
                int c = px[y * w + x];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                int chr = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
                if (chr > 18) chroma++;
                float l = lumOf(px, w, x, y);
                float gradMag = Math.abs(l - lumOf(px, w, x - 1, y)) + Math.abs(l - lumOf(px, w, x + 1, y))
                        + Math.abs(l - lumOf(px, w, x, y - 1)) + Math.abs(l - lumOf(px, w, x, y + 1));
                if (gradMag > 60f) microEdges++;
            }
        }
        if (total == 0) return 0f;
        float chromaFrac = chroma / (float) total;
        float edgeFrac = microEdges / (float) total;
        return clamp01(chromaFrac * 0.45f + edgeFrac * 4f * 0.55f);
    }

    static float clamp01(double v) { return (float) Math.max(0, Math.min(1, v)); }
    static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    // ===================== summary =====================

    static void printPerCategorySummary(Map<String, List<Result>> all) {
        System.out.println("===== Per-category negative-sample report =====");
        int totalAll = 0, totalLeak = 0;
        for (var e : all.entrySet()) {
            String cat = e.getKey();
            List<Result> rs = e.getValue();
            int leak = 0;
            var why = new LinkedHashMap<String, Integer>();
            for (Result r : rs) {
                if (r.passClose) leak++;
                else why.merge(r.failureReason, 1, Integer::sum);
            }
            totalAll += rs.size();
            totalLeak += leak;
            System.out.printf(Locale.ROOT, "%n[%s]  %d photos  -  %d false accept(s)  =  %d rejected%n",
                    cat, rs.size(), leak, rs.size() - leak);
            if (!why.isEmpty()) {
                System.out.print("   rejected by: ");
                why.forEach((k, v) -> System.out.printf("%s(%d)  ", k, v));
                System.out.println();
            }
            // Per-signal stats for this category.
            printCatStat(rs, "roadRatio",                r -> r.roadRatio);
            printCatStat(rs, "asphaltRatio",             r -> r.asphaltRatio);
            printCatStat(rs, "circularVesselScore",      r -> r.circularVesselScore);
            printCatStat(rs, "indoorFloorRatio",         r -> r.indoorFloorRatio);
            printCatStat(rs, "verticalStructureRatio",   r -> r.verticalStructureRatio);
            printCatStat(rs, "vegetationRatio",          r -> r.vegetationRatio);
            printCatStat(rs, "flatScreenLikelihood",     r -> r.flatScreenLikelihood);
            printCatStat(rs, "rectFrameLikelihood",      r -> r.rectFrameLikelihood);
            printCatStat(rs, "brightCircleRingScore",    r -> r.brightCircleRingScore);
            printCatStat(rs, "uniformWallScore",         r -> r.uniformWallScore);
            printCatStat(rs, "leafyTextureScore",        r -> r.leafyTextureScore);
        }
        System.out.printf(Locale.ROOT, "%n===== TOTAL: %d / %d photos still incorrectly accepted =====%n",
                totalLeak, totalAll);
    }

    interface Sel { float pick(Result r); }

    static void printCatStat(List<Result> rs, String label, Sel sel) {
        if (rs.isEmpty()) return;
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        double sum = 0;
        float[] vals = new float[rs.size()];
        int i = 0;
        for (Result r : rs) {
            float v = sel.pick(r);
            vals[i++] = v;
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        Arrays.sort(vals);
        float median = vals[vals.length / 2];
        System.out.printf(Locale.ROOT,
                "    %-26s  min=%.3f  med=%.3f  max=%.3f  mean=%.3f%n",
                label, min, median, max, sum / rs.size());
    }

    static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    static String csvSafe(String s) { return s.replace(',', ';'); }

    static class Result {
        String category, name;
        float roadRatio, asphaltRatio, indoorFloorRatio, woodPanelRatio;
        float verticalStructureRatio, skyBandRatio, highSaturationRatio;
        float circularVesselScore, horizontalRoadLineScore;
        float skinRatio, vegetationRatio, animalFurRatio, foodOrObjectColorRatio;
        float rectFrameLikelihood, flatScreenLikelihood, saturatedNonRoadRatio;
        float brightCircleRingScore, uniformWallScore, leafyTextureScore;
        boolean passClose;
        String failureReason = "";
    }
}
