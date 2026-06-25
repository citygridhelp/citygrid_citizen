#!/usr/bin/env python3
"""
Train YOLOv8 (or YOLOv8-Seg) for pothole detection and export to TensorFlow Lite
for the Pothole Report Android app.

The Android app's PotholeDetector auto-detects the model format, so you can drop
the exported `.tflite` straight into `app/src/main/assets/ml/pothole_detect.tflite`
and it will start using YOLOv8 inference automatically — no Kotlin changes needed.

------------------------------------------------------------------------------
Dataset layout (YOLO format, exactly what Roboflow exports):

  dataset/
    data.yaml
    images/train/*.jpg
    images/val/*.jpg
    labels/train/*.txt        # one .txt per image, YOLO format
    labels/val/*.txt
    # For YOLOv8-Seg, labels contain polygon points instead of xywh.

  data.yaml:
    path: <absolute path to dataset>
    train: images/train
    val: images/val
    names:
      0: pothole

Tip: easiest path is to label on Roboflow (https://roboflow.com), then
"Export dataset" → "YOLOv8" → "Show download code" → pick the local zip option.

------------------------------------------------------------------------------
Usage:

  # Detection model (lighter, recommended starting point)
  python train_yolov8.py --data dataset/data.yaml --model yolov8n.pt --epochs 80

  # Segmentation model (more accurate width/depth signals)
  python train_yolov8.py --data dataset/data.yaml --model yolov8n-seg.pt --epochs 80

  # Larger model for more accuracy (needs GPU)
  python train_yolov8.py --data dataset/data.yaml --model yolov8s.pt --epochs 100 --imgsz 640
"""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path

from ultralytics import YOLO


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--data",
        type=Path,
        required=True,
        help="Path to YOLO data.yaml",
    )
    parser.add_argument(
        "--model",
        type=str,
        default="yolov8n.pt",
        help="Base model: yolov8n.pt / yolov8s.pt / yolov8n-seg.pt / yolov8s-seg.pt",
    )
    parser.add_argument("--epochs", type=int, default=80)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--batch", type=int, default=16)
    parser.add_argument(
        "--output-dir", type=Path, default=Path("output"), help="Where to copy the final tflite"
    )
    parser.add_argument(
        "--int8",
        action="store_true",
        help="Quantize TFLite to INT8 (smaller / faster on phones; needs calibration data)",
    )
    parser.add_argument(
        "--no-aug",
        action="store_true",
        help="Disable extra augmentation (use default ultralytics aug only)",
    )
    args = parser.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)

    # ---------------- Train ----------------
    model = YOLO(args.model)
    train_kwargs = dict(
        data=str(args.data),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        patience=20,
        project=str(args.output_dir / "runs"),
        name="pothole",
        exist_ok=True,
    )

    # Strong augmentation helps on small Indian-road datasets.
    if not args.no_aug:
        train_kwargs.update(
            dict(
                hsv_h=0.02,
                hsv_s=0.50,
                hsv_v=0.40,
                degrees=8.0,
                translate=0.10,
                scale=0.5,
                shear=2.0,
                perspective=0.0005,
                fliplr=0.5,
                mosaic=1.0,
                mixup=0.10,
                copy_paste=0.10,  # Useful for segmentation models.
            )
        )

    results = model.train(**train_kwargs)
    best_pt = Path(model.trainer.best) if hasattr(model, "trainer") else None
    if best_pt is None or not best_pt.exists():
        best_pt = args.output_dir / "runs" / "pothole" / "weights" / "best.pt"
    print(f"Best weights: {best_pt}")

    # ---------------- Export to TFLite ----------------
    best = YOLO(str(best_pt))
    export_kwargs = dict(format="tflite", imgsz=args.imgsz)
    if args.int8:
        export_kwargs["int8"] = True
    exported_path = best.export(**export_kwargs)
    exported_path = Path(exported_path)
    print(f"Exported TFLite: {exported_path}")

    # ---------------- Stage outputs for the Android project ----------------
    final_tflite = args.output_dir / "pothole_detect.tflite"
    shutil.copyfile(exported_path, final_tflite)
    print(f"Staged: {final_tflite}")

    labels_path = args.output_dir / "pothole_labels.txt"
    # YOLOv8 expects class 0 = pothole. Keep label file aligned so the app's
    # legacy SSD path also stays compatible if someone toggles back.
    labels_path.write_text("Pothole\n", encoding="utf-8")
    print(f"Labels: {labels_path}")

    metrics_path = args.output_dir / "training_metrics.json"
    try:
        metrics_path.write_text(json.dumps(results.results_dict, indent=2, default=str))
    except Exception:
        metrics_path.write_text(json.dumps({"note": "metrics not serializable"}, indent=2))

    print()
    print("Next steps:")
    print(
        f"  Copy-Item {final_tflite} ..\\..\\app\\src\\main\\assets\\ml\\pothole_detect.tflite -Force"
    )
    print(
        f"  Copy-Item {labels_path} ..\\..\\app\\src\\main\\assets\\ml\\pothole_labels.txt -Force"
    )
    print("Then in Android Studio: Sync Gradle → Build → Rebuild Project → run.")


if __name__ == "__main__":
    main()
