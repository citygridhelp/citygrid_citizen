#!/usr/bin/env python3
"""
Train a binary pothole classifier and export to TensorFlow Lite for the Android app.

Dataset layout:
  dataset/train/pothole/*.jpg
  dataset/train/not_pothole/*.jpg   # bins, indoor floors, plain road, etc.
  dataset/val/pothole/
  dataset/val/not_pothole/

Usage:
  python train_and_export.py --data-dir dataset --epochs 25
"""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers


def build_model(img_size: int = 224) -> keras.Model:
    base = keras.applications.MobileNetV2(
        input_shape=(img_size, img_size, 3),
        include_top=False,
        weights="imagenet",
    )
    base.trainable = False
    inputs = keras.Input(shape=(img_size, img_size, 3))
    x = base(inputs, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.35)(x)
    x = layers.Dense(128, activation="relu")(x)
    outputs = layers.Dense(1, activation="sigmoid")(x)
    return keras.Model(inputs, outputs)


def make_datasets(data_dir: Path, img_size: int, batch_size: int):
    train_dir = data_dir / "train"
    val_dir = data_dir / "val"
    for split in (train_dir, val_dir):
        if not (split / "pothole").is_dir() or not (split / "not_pothole").is_dir():
            raise SystemExit(
                f"Missing folders. Need {split}/pothole and {split}/not_pothole"
            )

    train_ds = keras.utils.image_dataset_from_directory(
        train_dir,
        labels=("pothole", "not_pothole"),
        label_mode="binary",
        image_size=(img_size, img_size),
        batch_size=batch_size,
        shuffle=True,
    )
    val_ds = keras.utils.image_dataset_from_directory(
        val_dir,
        labels=("pothole", "not_pothole"),
        label_mode="binary",
        image_size=(img_size, img_size),
        batch_size=batch_size,
        shuffle=False,
    )
    autotune = tf.data.AUTOTUNE
    train_ds = train_ds.prefetch(autotune)
    val_ds = val_ds.prefetch(autotune)
    return train_ds, val_ds


def export_tflite(model: keras.Model, out_path: Path) -> None:
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    out_path.write_bytes(tflite_model)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", type=Path, default=Path("dataset"))
    parser.add_argument("--epochs", type=int, default=25)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--img-size", type=int, default=224)
    parser.add_argument("--output-dir", type=Path, default=Path("output"))
    args = parser.parse_args()

    out_dir = args.output_dir
    out_dir.mkdir(parents=True, exist_ok=True)

    train_ds, val_ds = make_datasets(args.data_dir, args.img_size, args.batch_size)
    model = build_model(args.img_size)
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=1e-4),
        loss="binary_crossentropy",
        metrics=["accuracy"],
    )

    callbacks = [
        keras.callbacks.EarlyStopping(patience=5, restore_best_weights=True),
        keras.callbacks.ModelCheckpoint(
            out_dir / "best.keras",
            save_best_only=True,
        ),
    ]

    history = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=args.epochs,
        callbacks=callbacks,
    )

    metrics = {k: [float(x) for x in v] for k, v in history.history.items()}
    (out_dir / "training_metrics.json").write_text(json.dumps(metrics, indent=2))

    export_tflite(model, out_dir / "pothole_detect.tflite")
    shutil.copyfile(out_dir / "pothole_detect.tflite", out_dir / "pothole_classifier.tflite")

    labels_path = out_dir / "pothole_labels.txt"
    labels_path.write_text("not_pothole\npothole\n", encoding="utf-8")

    print("Done.")
    print(f"  TFLite: {out_dir / 'pothole_detect.tflite'}")
    print(f"  Labels: {labels_path}")
    print("Copy both into app/src/main/assets/ml/ and rebuild the APK.")
    print()
    print("Note: This script trains a BINARY classifier (sigmoid output).")
    print("The current app SSD expects detection scores — for best results,")
    print("add many not_pothole images (bins, indoor) and tune app thresholds,")
    print("or export an SSD model via TensorFlow Model Maker object detection.")


if __name__ == "__main__":
    main()
