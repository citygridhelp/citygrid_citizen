# Drop your close-up pothole photos here

This folder is used to **calibrate** the heuristic validator and content
classifier inside the running APK against real Indian close-up pothole
photos — it is NOT used for model training.

## How to use

1. Drop your close-up pothole photos into this folder. Any common image
   format works (`.jpg`, `.jpeg`, `.png`, `.webp`).
2. Tell the AI agent in chat: "Tune heuristics against the photos in
   `tools/train_pothole_model/dataset/calibration_close_up/`".
3. The agent will:
   - Read each photo, compute its `SceneAnalysis` and `ContentAnalysis`
     fingerprint.
   - Adjust thresholds in `PotholePhotoValidator` and `ContentClassifier`
     so each photo passes the close-up validator.
   - Verify rejection still works for bins / indoor floors / people /
     animals / printed photos / phone screens.
4. The same photos should ALSO be copied to `dataset/images/train/`
   (alongside YOLO-format `.txt` labels in `dataset/labels/train/`) once
   you're ready to train a real model with `train_yolov8.py`. The agent
   can do this copy step for you.

## What this folder is NOT

- Not a training set — there are no labels here.
- Not used by `train_yolov8.py`.
- Not bundled in the APK.

## Privacy note

These photos may contain GPS metadata. If you intend to share the dataset
publicly, strip EXIF first. The Android app itself never uploads these.
