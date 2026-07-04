# City Grid citizen app — ML models, features & evaluation

Reference for **how pothole photos are classified and analyzed** in the citizen app (`potholereport`), what signals are used, what thresholds apply, and **how accuracy is measured today vs planned**.

Related:

- [`pothole_classification_model.md`](pothole_classification_model.md) — standalone doc: classification models, features, thresholds, metrics
- [`tools/train_pothole_model/CALIBRATION_NOTES.md`](../tools/train_pothole_model/CALIBRATION_NOTES.md) — threshold change log + measured pass/reject rates
- [`tools/train_pothole_model/README.md`](../tools/train_pothole_model/README.md) — YOLOv8 training & mAP targets
- [`product_assumptions.md`](product_assumptions.md) — duplicate radius, GPS, etc.
- [`tools/calibrate/`](../tools/calibrate/) — Java calibration tools + CSV outputs

*Last reviewed: July 2026*

---

## 1. Executive summary

| Layer | Technology in **current production build** | Neural model active? |
|-------|------------------------------------------|----------------------|
| Photo accept/reject (close-up + wide) | Pixel **heuristics** (`SceneHeuristics`, `ContentClassifier`, `PotholeBlobLocator`) | **No** — `PotholePhotoValidator` explicitly does not call `PotholeDetector` |
| Risk insight (width / depth / suggested severity) | **Heuristic** blob + formulas (`PotholeRiskAnalyzer`) | **No** — unless `CloudPotholeAnalysisClient` is registered |
| On-device detector (optional asset) | `PotholeDetector` → `assets/ml/pothole_detect.tflite` | **Not shipped** in repo (only `pothole_labels.txt` present); code ready for YOLOv8 / SSD |
| Monocular depth (optional asset) | `MonocularDepthEstimator` → `assets/ml/depth.tflite` | **Not shipped**; optional future fusion |
| Cloud analysis | `CloudPotholeAnalysisClient` interface | **Not implemented** — hook only |

**UI note:** New report screen copy says photos are checked with “TensorFlow Lite”. The **validator path is heuristic-only** today; LiteRT/TFLite is on the classpath for optional/future `pothole_detect.tflite` inference.

---

## 2. Pipeline overview

```text
User captures close-up (+ wide) photo
        │
        ▼
┌───────────────────────────────────────┐
│  PotholePhotoValidator.validate()      │
│  • SceneHeuristics (road / indoor…)    │
│  • ContentClassifier (person/plant…)   │
│  • PotholeBlobLocator (negative blob)  │
│  • RoadSceneHeuristics (wide vs close) │
└───────────────────────────────────────┘
        │ Accepted
        ▼
┌───────────────────────────────────────┐
│  PotholeRiskAnalyzer.analyze()         │
│  • User: PotholePosition (L/M/R)       │
│  • User: severity (optional input)     │
│  • PotholeBlobLocator (lane-limited)   │
│  • Risk score → width/depth/severity   │
└───────────────────────────────────────┘
        │
        ▼
User submits report (GPS + photos + severity + note)
```

**Future path (not active in validator today):**

```text
pothole_detect.tflite (YOLOv8-Seg) → PotholeDetector → mask/bbox → PotholeRiskAnalyzer.enrichWithMaskGeometry
CloudPotholeAnalysisClient → overrides local analyze() when installed
depth.tflite → MonocularDepthEstimator → depth contrast in mask (planned)
```

---

## 3. Models & classifiers

### 3.1 Active: heuristic photo gate (`PotholePhotoValidator`)

| Component | Type | Role |
|-----------|------|------|
| `SceneHeuristics` | Rule-based (72×72 sample) | Road / asphalt / indoor / sky / circular vessel / line cues |
| `ContentClassifier` | Rule-based (72×72 sample) | Reject people, animals, plants, screens, saturated objects |
| `PotholeBlobLocator` | Flood-fill blob (240 px edge) | Reject negative signatures (walls, pipes, garbage…) on close-up |
| `RoadSceneHeuristics` | Perceptual hash (Hamming) | Wide shot must differ from close-up (min distance **8**) |

**Classes (implicit):** binary **Accept** vs **Reject** with user-facing reason strings — not multi-class softmax.

### 3.2 Active: heuristic risk analyzer (`PotholeRiskAnalyzer`)

| Input | Output |
|-------|--------|
| Close-up bitmap + lane + optional user severity | `PotholeRiskInsight`: width cm, depth cm, suggested severity, speed advisory, criticality label |

Formulas (reference Pothole Watch port):

- Frame area assumption: **4800 cm²** (~80×60 cm road in frame)
- Width: `sqrt(areaPct/100 × 4800 × aspect)` clamped **8–180 cm**
- Depth: `contrast × 22` clamped **0.5–18 cm**
- Risk score: weighted blend (depth 30%, area 25%, contrast 15%, user severity 30%) → buckets at **75 / 55 / 35**

### 3.3 Shipped in code, optional asset: `PotholeDetector`

| Item | Detail |
|------|--------|
| Asset | `app/src/main/assets/ml/pothole_detect.tflite` |
| Runtime | LiteRT `Interpreter` (`com.google.ai.edge.litert:litert`) |
| Formats | Auto-detect **SSD**, **YOLOv8 det**, **YOLOv8-Seg** |
| Class label | `Pothole` (`pothole_labels.txt`) |
| Score threshold | **`YOLO_MIN_SCORE = 0.18`** |
| Status | **Not invoked** by `PotholePhotoValidator` in current build; `.tflite` not in repo |

Train/export: `tools/train_pothole_model/train_yolov8.py` (Ultralytics).

### 3.4 Optional: `MonocularDepthEstimator`

| Item | Detail |
|------|--------|
| Asset | `assets/ml/depth.tflite` (e.g. MiDaS-small) |
| Status | **Not bundled**; `tryLoad()` returns null if missing |
| Use | Planned depth contrast inside pothole mask when wired |

### 3.5 Future: `CloudPotholeAnalysisClient`

Interface only — remote model/API can replace local `PotholeRiskAnalyzer` when `install()` is called.

---

## 4. Independent vs dependent features

**Independent** — computed from the photo (and kind: close-up vs wide) only:

| Feature group | Signals | Source |
|---------------|---------|--------|
| **Scene** | `roadRatio`, `asphaltRatio`, `indoorFloorRatio`, `woodPanelRatio`, `verticalStructureRatio`, `skyBandRatio`, `highSaturationRatio`, `circularVesselScore`, `horizontalRoadLineScore` | `SceneHeuristics` |
| **Content** | `skinRatio`, `vegetationRatio`, `animalFurRatio`, `foodOrObjectColorRatio`, `rectFrameLikelihood`, `flatScreenLikelihood`, `saturatedNonRoadRatio` | `ContentClassifier` |
| **Blob (full frame)** | `areaPct`, `contrast`, `blobInsideAsphaltFrac`, `aspect`, `edgeTouches`, … | `PotholeBlobLocator` (validator uses `position = null`) |
| **Wide duplicate check** | Perceptual hash Hamming distance vs close-up | `RoadSceneHeuristics` |

**Dependent** — require user action, another photo, or upstream output:

| Feature | Depends on | Used in |
|---------|------------|---------|
| **`PotholePosition`** (LEFT / MIDDLE / RIGHT) | User tile selection; lane bands 0–0.42 / 0.30–0.70 / 0.58–1.0 | `PotholeRiskAnalyzer` blob search region |
| **`userSeverity`** | User “How bad is it?” selection | Risk score 30% weight |
| **Wide validation** | Requires verified **close-up URI** for hash + cross-check | `validateWide` |
| **Blob in lane** | Same bitmap as close-up **+** selected position | Risk analyzer only |
| **`PotholeDetection` / seg mask** | `PotholeDetector` or cloud (future) | `enrichWithMaskGeometry` (scaffolding, unused in heuristic path) |
| **`DepthMap`** | `depth.tflite` + mask (future) | Not active |
| **Cloud insight** | Network + registered client | Overrides local analyze |

---

## 5. Thresholds (production heuristic path)

### 5.1 Close-up gate — scene & content

| Rule | Threshold | File |
|------|-----------|------|
| Min road ratio | **≥ 0.13** | `PotholePhotoValidator` |
| Min asphalt ratio (or road lines) | asphalt **≥ 0.06** OR line score **≥ 0.05** | same |
| Circular vessel + low road | score **≥ 0.62** AND road **< 0.42** | same |
| Indoor floor false positive | indoor **≥ 0.50** AND road **< 0.30** | `SceneHeuristics.isLikelyNonRoadFalsePositive` |
| Skin reject | **≥ 0.45** | `ContentClassifier` |
| Vegetation reject | **≥ 0.32** | same |
| Animal fur reject | **≥ 0.55** | same |
| Flat screen reject | **≥ 0.62** | same |
| Food/object + saturated | **≥ 0.30** AND saturated **≥ 0.22** | same |
| Rectangular frame | **≥ 1.05** (effectively disabled) | same |

### 5.2 Close-up gate — blob rejection

| Symbol | Value | Purpose |
|--------|-------|---------|
| `BLOB_MAX_AREA_PCT` | **30.0** | Blob too large (wall/garbage) |
| `BLOB_MAX_CONTRAST` | **0.80** | Extreme contrast |
| `BLOB_MID_AREA_PCT` + `BLOB_MID_INSIDE_ASPHALT` | **18.0** / **0.50** | Large non-asphalt blob (cement pipes) |
| `BLOB_HARD_INSIDE_ASPHALT` + area floor | **0.18** / **4%** | Small off-asphalt blob |

### 5.3 Wide shot gate

| Rule | Threshold |
|------|-----------|
| Min road ratio | **≥ 0.24** |
| Hash vs close-up | Hamming **≥ 8** (reject if **< 8**) |
| Outdoor Indian road heuristics | `looksLikeOutdoorIndianRoad` composite rules |

### 5.4 Risk analyzer

| Item | Value |
|------|-------|
| Min blob area to analyze | **0.5%** of frame |
| Risk buckets | **≥75** CRITICAL, **≥55** SEVERE, **≥35** MODERATE, else MINOR |
| Speed advisory | 20 / 30 / 45 / 60 km/h |

### 5.5 Neural detector (when asset present)

| Item | Value |
|------|-------|
| YOLO min confidence | **0.18** |
| Seg mask binarize | **0.5** (in detector mask decode) |

Full change history: **`CALIBRATION_NOTES.md`**.

---

## 6. How accuracy is tested today

### 6.1 Heuristic validator — **not** accuracy / precision / recall in the ML sense

The production gate is tuned with **labeled photo sets** and **pass/reject counts**, not a sklearn-style confusion matrix in the app or CI.

**Calibration datasets (documented May 2026):**

| Set | Size | Purpose |
|-----|------|---------|
| Positive close-ups | **37 photos** (34 unique) | Must stay **accepted** |
| Negative samples | **53 photos**, 7 categories | Should be **rejected** |

**Measured outcomes after blob rules (`CALIBRATION_NOTES.md` Round 2):**

| Metric | Before | After |
|--------|--------|-------|
| Negatives rejected | 3/53 (**6%**) | 28/53 (**53%**) |
| Positives kept | 37/37 (**100%**) | 33/34 (**97%**) |

**Per-category negative catch rate (after):** Cement 14/17, Garbage 2/2, Manhole 2/8, Road blockers 2/2, Sign board 0/3, Trees 4/6, Wall 4/15.

**Tools & artifacts:**

| Tool | Output | What it measures |
|------|--------|------------------|
| `CalibrateNegatives.java` | `negatives_report.csv` | Per-category false-accept rate + signal distributions |
| `BlobSurvey.java` | `blob_survey.csv` | Grid search on blob rules vs pos/neg separation |
| `AnalyzeCompare.java` | `analyze_compare.csv` | OLD vs NEW width/depth analyzer on positives |
| `AnalyzeNew.java` | `analyze_new.csv` | Lane-aware analyzer variance |
| `Calibrate.java` | `calibration_report.csv` | Round-1 content/scene threshold sweep |

**Confusion matrix:** Can be **derived offline** from the CSVs (TP/TN/FP/FN on accept/reject), but **is not generated automatically** in repo scripts today. Treat reported numbers as **empirical pass/reject rates**, not formal precision/recall.

**Precision / recall / F1:** **Not computed** for the heuristic stack in production tooling. Tuning goal is **conservative**: prefer false accept of borderline negatives over false reject of real potholes (see manhole/wall note in `CALIBRATION_NOTES.md`).

### 6.2 YOLOv8 training — **uses** precision, recall, mAP (when model is trained)

When `pothole_detect.tflite` is produced via `train_yolov8.py`:

| Metric | Source | Target (README guidance) |
|--------|--------|---------------------------|
| **mAP@0.5** | Ultralytics `results.csv` / `training_metrics.json` | **≥ 0.70** production minimum; **≥ 0.85** ideal |
| **mAP@0.5:0.95** | Same | Standard COCO-style summary |
| **Precision / recall** | Per-class in Ultralytics logs | Used to pick best checkpoint |
| **Confusion matrix** | Ultralytics validation plots in `output/runs/pothole/` | Object-detection CM for class `Pothole` |

Export writes `output/training_metrics.json` from `results.results_dict`.

**Current repo state:** Dataset has **placeholder empty labels**; no committed `training_metrics.json` or trained `.tflite`. mAP/precision/recall apply **after** a training run, not to the live heuristic app.

### 6.3 In-app / field testing

| What | How |
|------|-----|
| Photo gate | Manual test matrix: real potholes, bins, manholes, indoor, duplicate wide/close |
| Risk numbers | Advisory only — compare to `analyze_new.csv` ranges on calibration set |
| End-to-end | Citizen smoke test in [`release_and_versioning_guide.md`](release_and_versioning_guide.md) §5.2 |

---

## 7. Accuracy / precision / recall — direct answers

| Question | Answer for **current citizen app** |
|----------|-----------------------------------|
| Is **accuracy** used on-device? | **No** single accuracy score shown to users or logged in production. |
| Is **precision** used for heuristics? | **No** — rules tuned by pass/reject counts on fixed photo sets. |
| Is **recall** used for heuristics? | **No** — same; one positive missed in Round 2 (33/34). |
| Is **confusion matrix** used? | **Not in app**; optional offline from calibration CSVs; **Ultralytics CM** when YOLO is trained. |
| Will neural metrics apply later? | **Yes** — mAP, precision, recall for `pothole_detect.tflite`; cloud model metrics TBD. |

---

## 8. Planned improvements (from calibration docs)

1. **Train YOLOv8-Seg** on Indian roads + 30% hard negatives → ship `pothole_detect.tflite`.
2. Wire **detector into validator** (or cloud) — retire pure heuristics after ~150–200 labelled photos (`CALIBRATION_NOTES.md`).
3. Implement **`CloudPotholeAnalysisClient`** for manholes/walls that overlap pothole blob signatures.
4. Add **automated confusion matrix** script over `calibration_close_up/` + `Pothole Negative samples/` when thresholds change.
5. Align UI copy with actual backend (heuristic vs LiteRT) when path stabilizes.

---

## 9. File reference

| Path | Role |
|------|------|
| `app/.../ml/PotholePhotoValidator.kt` | Photo accept/reject orchestration |
| `app/.../ml/SceneHeuristics.kt` | Road/scene independent features |
| `app/.../ml/ContentClassifier.kt` | Non-road content independent features |
| `app/.../ml/PotholeBlobLocator.kt` | Blob finder + asphalt overlap |
| `app/.../ml/PotholeRiskAnalyzer.kt` | Width/depth/severity advisory |
| `app/.../ml/PotholeDetector.kt` | Optional TFLite YOLO/SSD |
| `app/.../ml/MonocularDepthEstimator.kt` | Optional depth TFLite |
| `app/.../ml/CloudPotholeAnalysisClient.kt` | Cloud hook |
| `app/.../data/PotholePosition.kt` | User-dependent lane bands |
| `app/src/main/assets/ml/pothole_labels.txt` | Class name for detector |
| `tools/train_pothole_model/` | Training + calibration docs |
| `tools/calibrate/*.java` | Offline evaluation tools |

---

*When thresholds or models change, update this file and `CALIBRATION_NOTES.md` in the same change.*
