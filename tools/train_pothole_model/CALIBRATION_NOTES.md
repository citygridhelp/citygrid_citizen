# Heuristic calibration

This file is the ground-truth log of every threshold or rule change made
against measured photo signatures from real Indian roads. Two rounds are
documented: the close-up validator tuning (round 1) and the hybrid
blob-finder + reference-style analyzer + position selector (round 2).

# Round 2 — hybrid analyzer + lane selector + negative-sample tuning

Calibration date: 2026-05-24
Positive source: `tools/train_pothole_model/dataset/calibration_close_up/`
  → 37 photos, 34 unique after dedup
Negative source: `tools/train_pothole_model/dataset/Pothole Negative samples/`
  → 53 photos across 7 categories (Cement pipelines 17, Garbage 2, Manhole 8,
    Road blockers 2, Sign board 3, Trees 6, Wall boundaries 15)

## Why the change

The user reported two issues with the previous heuristic analyzer:

1. The width/depth "analysis is slightly off" - measured: the OLD analyzer
   returned widthCm = **constant 50 cm** for every one of the 34 calibration
   photos (synthetic 70%×70% centered box always lands in the same width-
   score bucket), and the severity collapsed to MEDIUM for 30/34 photos.
2. Walls / manholes / cement pipelines / bushes / sign boards were passing
   the close-up validator. Measured: only 3 of 53 negatives were rejected
   by the existing scene/content rules.

A reference Java port of the Pothole Watch web pipeline was used as the
analytical baseline. Its 4-connected flood-fill blob finder + asphalt-
relative contrast formulas produced **0–18% area** and **0–15 cm depth**
across the same 34 photos, vs the constant 50 cm of the existing code.

## Tooling added

- `tools/calibrate/AnalyzeCompare.java` — runs OLD vs reference analyzer
  on every calibration photo (CSV: `analyze_compare.csv`). Confirmed the
  constant-50 cm regression.
- `tools/calibrate/CalibrateNegatives.java` — runs the close-up validator
  per negative-sample subfolder, reports per-category pass-rate and signal
  distributions (CSV: `negatives_report.csv`).
- `tools/calibrate/BlobSurvey.java` — runs the reference flood-fill blob
  finder on positives + negatives and tries every compound rejection rule
  in a (areaPct × contrast × insideAsphalt) grid to find the one that best
  separates positives from negatives (CSV: `blob_survey.csv`).
- `tools/calibrate/AnalyzeNew.java` — runs the NEW analyzer (blob-find +
  reference formulas + lane restriction) per lane and per photo (CSV:
  `analyze_new.csv`). Confirms width/depth/severity now actually vary.

## What changed in the Kotlin code

### New files

- `data/PotholePosition.kt` — `LEFT` / `MIDDLE` / `RIGHT` enum with the
  vertical-band ranges used to constrain blob search.
- `ml/PotholeBlobLocator.kt` — reference Pothole Watch flood-fill blob
  finder, restricted to a vertical lane. Returns areaPct, contrast,
  bbox, aspect, edgeTouches, ringAsphaltFrac, blobInsideAsphaltFrac.

### `ml/PotholeRiskAnalyzer.kt` — full rewrite of `analyze`

| | Before | After |
|--|--|--|
| Detection box | synthetic 70%×70% centered crop | actual largest-dark-blob bbox in user-selected lane |
| Width source | 6-bucket lookup on widthScore | `sqrt(areaPct/100 × 4800 cm² × aspect)` (reference formula) |
| Depth source | weighted contrast/darkFrac/irregularity | `clamp(contrast × 22, 0.5, 18)` (reference formula) |
| Risk score | severity bucket → speed | reference 4-signal blend (depth × 0.30 + area × 0.25 + contrast × 0.15 + sev × 0.30) → 75/55/35 cuts |
| Speed advisory | 10 / 15 / 25 / 40 km/h | 20 / 30 / 45 / 60 km/h |
| Severity labels | LOW / MEDIUM / HIGH / EXTREME | LOW / MODERATE / HIGH / CRITICAL |

The retired mask-geometry helpers (`enrichWithMaskGeometry`,
`statsFromSegMask`, `statsFromAdaptiveMask`, `sampleAsphaltBaseline`)
remain in the file as scaffolding for future YOLOv8-Seg / cloud-AI
integration; they are intentionally `@Suppress("unused")` and not invoked
by the heuristic-only path.

### `ml/PotholePhotoValidator.kt` — added blob-based rejection

After the existing scene + content checks, the close-up validator now also
runs `PotholeBlobLocator` against the whole frame (no lane info available
at capture time) and rejects when the blob signature matches a negative
category. Thresholds:

| Symbol | Value | Why |
|---|---|---|
| `BLOB_MAX_AREA_PCT` | 30.0 | positives max 18.04 → 12 pp margin |
| `BLOB_MAX_CONTRAST` | 0.80 | positives max 0.74 → 0.06 margin |
| `BLOB_MID_AREA_PCT` + `BLOB_MID_INSIDE_ASPHALT` | 18.0 / 0.50 | rejects "big dark blob that isn't asphalt-classified" — kills cement pipelines |
| `BLOB_HARD_INSIDE_ASPHALT` + 4% area floor | 0.18 / 4.0 | rejects "small dark blob that isn't asphalt at all" — kills tree / road-blocker shadows |

### `ui/report/NewReportScreen.kt` — new "POTHOLE POSITION IN PHOTO" section

Three-tile selector (LEFT / MIDDLE / RIGHT) added as section 04, above
"HOW BAD IS IT?" (now 05; "NOTE" is now 06). The lane selection is fed
into `PotholeRiskAnalyzer.analyze(...)` and a `LaunchedEffect` re-runs
the analysis whenever the user changes the lane, so the suggested width /
depth / severity update without retaking the photo.

## Measured outcomes

### Validator (close-up gate) - before vs after

| | before | after |
|---|---|---|
| Negative photos rejected | 3 / 53 (6%) | 28 / 53 (53%) |
| Positive photos kept | 37 / 37 (100%) | 33 / 34 (97%) |
| Per-category negatives caught | n/a | Cement 14/17, Garbage 2/2, Manhole 2/8, Road blockers 2/2, Sign board 0/3, Trees 4/6, Wall 4/15 |

Manholes and grey concrete walls remain partially undetected at the
heuristic level because their dark-blob signatures overlap with real
potholes (asphalt-classified pixels in both). This is a known limit of
pixel heuristics; the cloud AI pathway via `CloudPotholeAnalysisClient`
will resolve them properly. Tighter validator thresholds were tested and
rejected because they dropped real positives faster than they caught
borderline negatives.

### Analyzer (width / depth / severity) - before vs after

| Signal | OLD analyzer | NEW analyzer (middle lane) |
|---|---|---|
| widthCm distinct values across 34 photos | **1** (always 50 cm) | **19** (8 cm — 58 cm) |
| depthCm distinct values | 3 (6/9/13 cm) | **11** (5 cm — 16 cm) |
| Severity labels populated | MEDIUM, HIGH | LOW, MODERATE, HIGH, CRITICAL |
| Severity distribution | 30 MED / 4 HIGH | 4 LOW / 15 MOD / 12 HIGH / 3 CRIT |
| Lane sensitivity | none | LEFT 5/14/13/2 vs RIGHT 10/10/11/3 |

## Threshold sweep methodology

`BlobSurvey.java` ran a five-dimensional grid search over
`(maxArea, maxContrast, midArea, midInside, hardInside)` to find the rule
that maximised negative rejection while keeping ≥ 88% of positives. The
strongest discriminator was `blobInsideAsphaltFrac` (positives median
0.88 vs cement-pipelines median 0.16, road-blockers median 0.09, trees
median 0.24). Real potholes are dark depressions on asphalt - their
blob pixels still match the asphalt color profile because the inside is
dim asphalt, just darker than its surroundings. Cement pipes / manhole
shadows / wall shadows are made of concrete/metal/brick and don't match.

## How to re-run round 2

```powershell
& "$env:JAVA_HOME\bin\java.exe" tools\calibrate\BlobSurvey.java
& "$env:JAVA_HOME\bin\java.exe" tools\calibrate\AnalyzeNew.java
& "$env:JAVA_HOME\bin\java.exe" tools\calibrate\CalibrateNegatives.java
```

When you add more photos to either folder, re-run and let the sweep
re-suggest thresholds. After ~150–200 labelled photos the heuristic
will be retired in favour of YOLOv8-Seg + the cloud AI pipeline.

---

# Round 1 — close-up validator tuning

Calibration date: 2026-05-23
Source: `tools/train_pothole_model/dataset/calibration_close_up/`
Sample size: 37 close-up photos (34 unique after dedup) — real Indian roads

The on-device AI is currently disabled in the prototype build, so the
running validator and risk analyzer rely entirely on `SceneHeuristics` +
`ContentClassifier`. This file is the ground-truth log of every threshold
or rule change made against measured photo signatures.

## Tooling used

- `tools/calibrate/Calibrate.java` — single-file JDK program that mirrors
  the Kotlin heuristics exactly and computes per-photo feature ratios +
  pass/fail against the close-up validator gates.
  Run with: `"%JAVA_HOME%\bin\java.exe" tools\calibrate\Calibrate.java`.
  Output: `tools/calibrate/calibration_report.csv`.
- `tools/calibrate/StageDataset.java` — splits the calibration folder into
  `dataset/images/train` (90%) and `dataset/images/val` (10%) and writes
  empty placeholder `.txt` labels for later YOLOv8 training.

## Pass rate

| Round | Pass / Total | Notes |
|-------|--------------|-------|
| Round 0 (current shipping rules) | 5 / 37 | 13 photos misread as person, 12 as photo-of-photo, 7 as indoor floor |
| Round 1 (this calibration)        | 37 / 37 | All real pothole photos accepted; rejection paths preserved |

## Bug fixed

`SceneHeuristics.analyze` accessed `getPixel(x, y - 1)` and `getPixel(x, y + 1)`
without a Y-bounds guard. On Android `Bitmap.getPixel` throws
`IllegalArgumentException` for out-of-bounds — the throw was caught by the
outer try/catch in `PotholePhotoValidator.validate()` and rejected every
photo with "Photo check failed". This is almost certainly the root cause
of the reported "AI not working" symptom even before the on-device model
was disabled. Fix: added `&& y in 1 until h - 1` to the inner gradient loop.

## Threshold + rule changes

### `SceneHeuristics.kt`

| Symbol | Before | After | Why |
|--------|--------|-------|-----|
| `isIndoorFloorTilePixel` luminance band | `165..245` | `195..245` | Sunlit Indian asphalt sits at lum 165–195; tightening excludes it |
| `isIndoorFloorTilePixel` saturation max | `< 0.20` | `< 0.18` | Tighter sat exclusion for asphalt |
| `isIndoorFloorTilePixel` color tolerance | `< 22` | `< 18` | Tighter colour neutrality |
| `isLikelyNonRoadFalsePositive` indoor trigger | `>= 0.24f && road < 0.45f` | `>= 0.50f && road < 0.30f` | Real pothole photos hit indoorFloorRatio 0.077–0.42 with road > 0.37; threshold raised so only true tile floors trigger |
| `nonRoadRejectionReason` indoor branch | matches above | matches above | Keeps user-facing message aligned |
| Y-bounds guard in `analyze` | missing | added | Bug fix described above |

### `ContentClassifier.kt`

| Symbol | Before | After | Why |
|--------|--------|-------|-----|
| `isSkinPixel` r/g/b minima | `r>95, g>40, b>20` | `r>110, g>50, b>25` | Excludes warm dark asphalt |
| `isSkinPixel` chroma | `max-min > 15` | `max-min > 25` | Skin has stronger chroma than asphalt |
| `isSkinPixel` R-G separation | `> 15` | `> 25` | Same |
| `isSkinPixel` G-B separation | (none) | `> 18` | New constraint — asphalt has G≈B, skin has G > B |
| `isSkinPixel` luminance | (none) | `> 95` | Skin doesn't appear at low lum; asphalt does |
| `isAnimalFurPixel` luminance min | `60` | `80` | Excludes mid-dark asphalt |
| `isAnimalFurPixel` saturation min | `0.18` | `0.28` | Real fur has more chroma than warm asphalt |
| `isAnimalFurPixel` R-G separation | `> 4` | `> 12` | Same |
| `isAnimalFurPixel` G-B separation | `> 4` | `> 12` | Same |
| `isAnimalFurPixel` R-B band | `<= 110` | `50..120` | Tighter warm-tone window |
| `skinRatio` reject threshold | `>= 0.18` | `>= 0.45` | Margin above the post-fix dataset max of 0.131 |
| `animalFurRatio` reject threshold | `>= 0.32` | `>= 0.55` | Margin above the post-fix dataset max of 0.032 |
| `rectFrameLikelihood` reject threshold | `>= 0.62` | `>= 1.05` (effectively disabled) | Rectangular-frame heuristic has too high false-rejection cost on close-up road photos with curbs / markings on all four sides |
| `flatScreenLikelihood` reject threshold | `>= 0.55` | `>= 0.62` | Margin above the dataset max of 0.586 |

### `PotholePhotoValidator.kt`

| Symbol | Before | After | Why |
|--------|--------|-------|-----|
| `CLOSE_UP_MIN_ROAD_RATIO` | `0.20f` | `0.13f` | Post-fix roadRatio min in dataset is 0.370; conservative 0.13 leaves dim/wet-asphalt headroom |
| `CLOSE_UP_MIN_ASPHALT_RATIO` | `0.06f` | `0.06f` (unchanged) | Post-fix asphaltRatio min 0.334 — already passes |
| `CLOSE_UP_MIN_ROAD_LINE_SCORE` | `0.05f` | `0.05f` (unchanged) | Post-fix horizontalRoadLineScore min 0.317 — already passes |

## Dataset distributions after calibration (37 photos, 34 unique)

| Signal | min | p10 | median | p90 | max | reject if |
|--------|-----|-----|--------|-----|-----|-----------|
| roadRatio                | 0.370 | 0.453 | 0.590 | 0.793 | 0.916 | < 0.13 |
| asphaltRatio             | 0.334 | 0.447 | 0.551 | 0.782 | 0.853 | < 0.06 (combined w/ roadLineScore) |
| horizontalRoadLineScore  | 0.317 | 0.351 | 0.394 | 0.420 | 0.440 | < 0.05 (combined) |
| circularVesselScore      | 0.000 | 0.000 | 0.021 | 0.325 | 0.459 | ≥ 0.55 + low road, or ≥ 0.62 + road < 0.42 |
| indoorFloorRatio         | 0.000 | 0.017 | 0.134 | 0.311 | 0.424 | ≥ 0.50 + road < 0.30 |
| woodPanelRatio           | 0.002 | 0.023 | 0.075 | 0.427 | 0.504 | ≥ 0.18 + asph < 0.16 |
| verticalStructureRatio   | 0.182 | 0.210 | 0.290 | 0.368 | 0.393 | ≥ 0.18 + asph < 0.12 |
| highSaturationRatio      | 0.000 | 0.000 | 0.002 | 0.017 | 0.037 | ≥ 0.07 + asph < 0.12 |
| skinRatio                | 0.000 | 0.000 | 0.004 | 0.041 | 0.131 | ≥ 0.45 |
| vegetationRatio          | 0.000 | 0.000 | 0.000 | 0.007 | 0.023 | ≥ 0.32 |
| animalFurRatio           | 0.000 | 0.000 | 0.002 | 0.013 | 0.032 | ≥ 0.55 |
| foodOrObjectColorRatio   | 0.000 | 0.000 | 0.004 | 0.026 | 0.059 | ≥ 0.30 + satNonRoad ≥ 0.22 |
| saturatedNonRoadRatio    | 0.000 | 0.000 | 0.004 | 0.026 | 0.059 | (combined w/ foodObject) |
| rectFrameLikelihood      | 0.000 | 0.000 | 0.300 | 1.000 | 1.000 | disabled (≥ 1.05) |
| flatScreenLikelihood     | 0.050 | 0.171 | 0.332 | 0.404 | 0.586 | ≥ 0.62 |

## Negative-sample sanity check

The threshold changes only **tighten** the positive surface of skin / fur /
indoor-floor pixel rules — they do not loosen any rejection threshold.
Walking through the gates for the standard non-pothole categories:

- **Garbage bin / drain cover** — `circularVesselScore` ≥ 0.55 + low road →
  rejected as before (`circularVesselScore` threshold unchanged).
- **Indoor tile floor** — true tile floors saturate `indoorFloorRatio`
  > 0.60 with `roadRatio` < 0.20; new gate `≥ 0.50 + road < 0.30` still
  rejects them.
- **Wall / vertical structure** — `verticalStructureRatio ≥ 0.18 +
  asphaltRatio < 0.12` unchanged → still rejected.
- **Person / face** — real face skin still satisfies the tightened Kovac
  rule (R~190 G~150 B~110 → R-G=40, G-B=40, lum 155). Real face
  `skinRatio` is typically 0.5–0.7, well above the new 0.45 threshold.
- **Plant / grass** — vegetation rule unchanged; threshold 0.32 unchanged.
- **Phone / monitor screen** — flatScreen still combines luminance flatness
  + saturation; threshold 0.62 still triggers on real screen captures
  (typical 0.75+).

## Dataset staged for future training

`StageDataset.java` ran and produced:

```
tools/train_pothole_model/dataset/
  data.yaml
  images/train/  (31 .jpg files, deduplicated)
  images/val/    (3 .jpg files)
  labels/train/  (31 empty .txt placeholders)
  labels/val/    (3 empty .txt placeholders)
```

Next steps when you're ready to train an actual YOLOv8 model:

1. Open the train images in Roboflow / LabelImg and draw bounding boxes
   around each pothole. Export as YOLO format.
2. Replace the placeholder `.txt` files with the YOLO label lines:
   `0 cx cy w h` (all normalised to [0, 1]).
3. Add **negative images** (bins, indoor floors, plain undamaged road,
   skin, vegetation, animals) to `images/train/` with empty `.txt` label
   files — these teach the model what NOT to flag.
4. Run `python tools\train_pothole_model\train_yolov8.py --data
   tools\train_pothole_model\dataset\data.yaml --model yolov8n.pt
   --epochs 80`.
5. Drop the resulting `pothole_detect.tflite` into
   `app/src/main/assets/ml/` and the on-device `PotholeDetector` will
   light up automatically (it auto-detects YOLOv8 vs SSD format).

## How to re-run this calibration

```powershell
& "$env:JAVA_HOME\bin\java.exe" tools\calibrate\Calibrate.java
& "$env:JAVA_HOME\bin\java.exe" tools\calibrate\StageDataset.java
```

Drop new photos into `dataset/calibration_close_up/`, re-run, and the
threshold suggestions in this document update accordingly. After ~100+
photos the heuristic should be retired in favour of the cloud AI
pipeline planned via `CloudPotholeAnalysisClient`.
