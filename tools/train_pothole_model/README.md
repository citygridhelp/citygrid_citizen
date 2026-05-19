# Train & update the pothole AI model (outside the APK)

The app loads the model from:

```
app/src/main/assets/ml/pothole_detect.tflite
app/src/main/assets/ml/pothole_labels.txt
```

You train on a PC, then copy the new `.tflite` into the project and ship a new APK (or app update).

## 1. Install Python (Windows)

1. Install [Python 3.10+](https://www.python.org/downloads/) and check **Add to PATH**.
2. Open PowerShell in this folder:

```powershell
cd tools\train_pothole_model
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## 2. Prepare images

Create this folder layout (add **many** examples per class):

```
dataset/
  train/
    pothole/          # real potholes on roads (India streets/highways)
    not_pothole/      # NEGATIVE examples — critical to reduce mistakes
  val/
    pothole/
    not_pothole/
```

### Put these in `not_pothole/` (important)

- Garbage bins / dustbins (like your test photo)
- Indoor tiled floors and rooms
- Drains, manhole covers, shadows on pavement
- Plain road with no damage
- Footpaths, sidewalks without potholes
- Vehicles, people, buildings only

**Tip:** Include every mistake the app makes — retrain until those photos stay rejected.

## 3. Train & export

```powershell
python train_and_export.py --data-dir dataset --epochs 25
```

Outputs:

- `output/pothole_detect.tflite` — copy into the Android project
- `output/pothole_labels.txt`
- `output/training_metrics.json`

## 4. Install the new model in the app

```powershell
Copy-Item output\pothole_detect.tflite `
  ..\..\app\src\main\assets\ml\pothole_detect.tflite -Force
Copy-Item output\pothole_labels.txt `
  ..\..\app\src\main\assets\ml\pothole_labels.txt -Force
```

Then in Android Studio:

1. **File → Sync Project with Gradle Files**
2. **Build → Clean Project**
3. **Build → Rebuild Project**
4. Run on device or build a new release APK / Play Store update

No Kotlin code changes are needed if the file names stay the same.

## 5. Optional: object-detection model (SSD)

The current app uses an SSD-style model (`detect.tflite`) with label `Pothole`. For a custom SSD you can use [TensorFlow Model Maker](https://www.tensorflow.org/lite/models/modify/model_maker/object_detection) or the [LiteRT / TFLite object detection workflow](https://ai.google.dev/edge/litert/libraries/modify/object_detection), then export to `.tflite` and replace the same asset path.

## 6. App-side rules (already in the APK)

Even with a better model, the app also checks:

- **Close-up:** road/asphalt visible; rejects bins, indoor floors, walls
- **Wide shot:** must look like an outdoor Indian road/street/highway

Retraining improves ML accuracy; heuristics block obvious non-road photos.
