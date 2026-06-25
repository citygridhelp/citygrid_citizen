/*
 * Stage the calibration photos into the YOLOv8 dataset folder used by
 * tools/train_pothole_model/train_yolov8.py.
 *
 * - Reads from tools/train_pothole_model/dataset/calibration_close_up/
 * - Deduplicates by content hash so gallery duplicates ("IMG... 1.jpg") are
 *   collapsed.
 * - Splits 90% train / 10% val deterministically by sorted filename.
 * - Copies images to dataset/images/{train,val}/ and creates empty
 *   placeholder labels in dataset/labels/{train,val}/ — you fill those
 *   with YOLO bounding-box lines once the photos are actually labelled
 *   (Roboflow / LabelImg / etc).
 *
 * Run with the JDK that Gradle uses:
 *   "%JAVA_HOME%\bin\java.exe" tools\calibrate\StageDataset.java
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StageDataset {

    public static void main(String[] args) throws Exception {
        Path source = Paths.get("tools/train_pothole_model/dataset/calibration_close_up");
        Path datasetRoot = Paths.get("tools/train_pothole_model/dataset");
        Path imgTrain = datasetRoot.resolve("images/train");
        Path imgVal = datasetRoot.resolve("images/val");
        Path lblTrain = datasetRoot.resolve("labels/train");
        Path lblVal = datasetRoot.resolve("labels/val");
        for (Path p : new Path[]{imgTrain, imgVal, lblTrain, lblVal}) Files.createDirectories(p);

        if (!Files.isDirectory(source)) {
            System.err.println("Source folder not found: " + source.toAbsolutePath());
            System.exit(2);
        }

        List<File> files = new ArrayList<>();
        try (var s = Files.list(source)) {
            s.filter(p -> {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp");
            }).sorted().forEach(p -> files.add(p.toFile()));
        }

        Set<String> hashes = new HashSet<>();
        List<File> uniques = new ArrayList<>();
        for (File f : files) {
            if (hashes.add(quickHash(f))) uniques.add(f);
        }

        int total = uniques.size();
        int valCount = Math.max(1, Math.round(total * 0.10f));
        int trainCount = total - valCount;
        int copiedTrain = 0, copiedVal = 0;

        System.out.printf(Locale.ROOT,
                "Source images: %d (%d unique after dedup) -> %d train / %d val%n",
                files.size(), total, trainCount, valCount);

        for (int i = 0; i < total; i++) {
            File src = uniques.get(i);
            boolean isVal = i >= trainCount;
            String safe = sanitize(src.getName());
            Path imgDst = (isVal ? imgVal : imgTrain).resolve(safe);
            Path lblDst = (isVal ? lblVal : lblTrain).resolve(stripExt(safe) + ".txt");

            Files.copy(src.toPath(), imgDst, StandardCopyOption.REPLACE_EXISTING);
            if (!Files.exists(lblDst)) {
                Files.writeString(lblDst, "");
            }
            if (isVal) copiedVal++;
            else copiedTrain++;
        }

        System.out.printf(Locale.ROOT, "Staged %d train images, %d val images.%n", copiedTrain, copiedVal);
        System.out.println("Empty .txt placeholders written - replace with YOLO bounding boxes");
        System.out.println("(class 0 = pothole; format: '0 cx cy w h' normalised 0..1).");
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

    static String sanitize(String name) {
        return name.replace(' ', '_').replace(',', '_');
    }

    static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }
}
