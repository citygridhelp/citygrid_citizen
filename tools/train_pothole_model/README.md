# Train & update the pothole AI model (outside the APK)

The app loads its detection model from:

```
app/src/main/assets/ml/pothole_detect.tflite
app/src/main/assets/ml/pothole_labels.txt
```

The Android side (`PotholeDetector.kt`) **auto-detects** whether that `.tflite` is
an SSD model (legacy) or a **YOLOv8 / YOLOv8-Seg** model — so you can switch
backends just by replacing the asset.

Recommended (best accuracy on Indian roads, mask-friendly for width/depth):

> **YOLOv8-Seg** (`yolov8n-seg.pt` → fine-tuned → `.tflite`)
>
> The Android side **decodes the YOLOv8-Seg proto masks** for real
> per-pothole segmentation and feeds the mask into `PotholeRiskAnalyzer` to
> produce true mask-based width / depth signals. When no segmentation mask is
> available (e.g. `yolov8n` detection or the legacy SSD), the analyzer falls
> back to an adaptive in-box mask, so quality degrades gracefully — you never
> *need* the seg model, but you'll get the best results with it.

---

## 1. Install Python (Windows)

1. Install [Python 3.10 or 3.11](https://www.python.org/downloads/) (Ultralytics
   does not yet support 3.12+ TFLite export cleanly) and check **Add to PATH**.
2. Open PowerShell in this folder:

```powershell
cd tools\train_pothole_model
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install --upgrade pip
pip install -r requirements.txt
```

> A CUDA-capable GPU is highly recommended (10–20× faster training). CPU works
> too for small datasets but will be slow.

---

## 2. Prepare the dataset (YOLO format)

Easiest workflow: **Roboflow** (free tier).

1. Create a Roboflow project → "Object Detection" (or "Instance Segmentation"
   if you want YOLOv8-Seg).
2. Upload your collected pothole photos (real Indian roads — close-up + wide).
3. Annotate potholes with boxes (or polygons for seg).
4. Click "Export Dataset" → format: **YOLOv8** → "Show download code" → "Download zip".
5. Unzip into `tools/train_pothole_model/dataset/`. You should get:

```
dataset/
  data.yaml
  images/train/*.jpg
  images/val/*.jpg
  labels/train/*.txt
  labels/val/*.txt
```

### Critical: include strong negatives

Add images **without potholes** to the training set (use Roboflow's "null /
background" image feature or add empty `.txt` files). Include:

- Garbage bins / dustbins (the original false-positive trigger)
- Drains, manhole covers, shadows
- Plain undamaged asphalt
- Indoor tiled floors, footpaths, walls
- Vehicles, people, buildings (no road)

A good starting target is **~1500–3000 pothole images + ~30% negatives**, with a
roughly 85% / 15% train / val split.

### Quick augmentation tips inside Roboflow

- Brightness ±25%
- Saturation ±20%
- Blur up to 1.5px
- Rotation ±10°
- Mosaic / cutout (Roboflow advanced)

---

## 3. Train & export to TFLite

Detection (lighter, fast on phones):

```powershell
python train_yolov8.py --data dataset\data.yaml --model yolov8n.pt --epochs 80
```

Segmentation (better mask-based width/depth):

```powershell
python train_yolov8.py --data dataset\data.yaml --model yolov8n-seg.pt --epochs 80
```

Larger / more accurate (needs GPU + more data):

```powershell
python train_yolov8.py --data dataset\data.yaml --model yolov8s.pt --epochs 100 --imgsz 640
```

Optional INT8 quantization (smaller / faster phone inference):

```powershell
python train_yolov8.py --data dataset\data.yaml --model yolov8n.pt --epochs 80 --int8
```

The script writes to `output/`:

- `output/pothole_detect.tflite` — drop into the Android project
- `output/pothole_labels.txt`
- `output/training_metrics.json`
- `output/runs/pothole/` — full Ultralytics run folder (mAP curves, sample preds)

---

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
4. Run on device, or build a new release APK / Play Store update.

No Kotlin code changes are needed — `PotholeDetector` reads the model's tensor
shapes and switches to YOLOv8 parsing automatically.

---

## 5. How to push max accuracy (training-side)

1. **Data > model size.** 3× more images beats moving from `yolov8n` → `yolov8s`.
2. **Hard-negative mining.** Every photo the app falsely flags or falsely
   rejects → re-add it (correctly labelled) to the next training set.
3. **Focus on Indian roads.** Diversity matters: city tar, highway concrete,
   rural laterite, monsoon-wet roads, sunlit dusty roads, night/low-light.
4. **Augmentation.** Keep mosaic + mixup on. Add HSV saturation / brightness to
   simulate Indian sun and dust.
5. **Two-stage training.** Train at `imgsz=640`, then fine-tune 10–20 epochs at
   `imgsz=960` for sharper geometry.
6. **Track mAP@0.5** in `output/runs/pothole/results.csv` — target ≥ 0.70 for
   safe production use, ≥ 0.85 for ideal.
7. **Quantize last.** Always validate FP32 mAP first, then run `--int8` and
   verify the drop is < 2–3 mAP points before shipping.

---

## 6. Optional: depth estimation (advanced)

The Android `PotholeRiskAnalyzer` already estimates depth from luminance
contrast inside the detection mask. If you want a **true monocular depth
model** to further refine the depth score:

1. Get a small depth TFLite model — recommended:
   - [MiDaS small TFLite](https://github.com/isl-org/MiDaS) (`midas_v2_small.tflite`)
   - or [Depth-Anything-v2 small TFLite](https://github.com/DepthAnything/Depth-Anything-V2)
2. Rename it to `depth.tflite`.
3. Drop it into the app at:

```
app/src/main/assets/ml/depth.tflite
```

That's it. The app **auto-detects the file at runtime** via
`MonocularDepthEstimator.tryLoad()` and fuses the depth-map signal into the
depth score automatically — no Kotlin changes required. If the file is
absent, the analyzer keeps using luminance contrast.

Constraints:

- Input must be `FP32` in `[0, 1]` RGB (the default for most depth TFLite exports).
- Output must be a single-channel relative depth map at any size; the
  estimator handles `[1, h, w]`, `[1, 1, h, w]` and `[1, h, w, 1]` layouts.
- For ImageNet-normalized models, bake the normalization into the graph
  before export (or fork `MonocularDepthEstimator.kt` to do it).

---

## 7. App-side guarantees (already in the APK)

Even with a better model, the app also enforces:

- **Close-up photo:** must show road / asphalt; rejects bins, indoor floors, walls.
- **Wide shot:** must look like an outdoor Indian road / street / highway.
- **Mask priority:** YOLOv8-Seg proto masks are decoded on-device for true
  per-pixel pothole segmentation. If unavailable, the analyzer falls back to
  adaptive in-box thresholding so estimates never break.
- **Depth fusion:** if `assets/ml/depth.tflite` is present, the monocular
  depth-map signal is fused into the depth score automatically.

---

## 8. Legacy: binary classifier

The original `train_and_export.py` (binary MobileNetV2 classifier) is still in
this folder for reference, but is **superseded by `train_yolov8.py`** for the
detection use-case.
