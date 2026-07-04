# City Grid citizen app — pothole classification model

Standalone reference for **how the citizen app decides whether a photo is a valid road pothole** (`in.citygrid.citizen`), which models and features are involved, what thresholds apply, and how output quality is measured.

Broader ML pipeline (risk sizing, depth, planned cloud): [`ml_models_and_evaluation.md`](ml_models_and_evaluation.md)

*Last reviewed: July 2026*

---

## 1. Classification task

| Item | Detail |
|------|--------|
| **App** | City Grid citizen (`in.citygrid.citizen`) |
| **Input** | Close-up photo (required) + wide road photo (required) |
| **Output** | Binary **Accept** or **Reject**, with a user-facing reason |
| **Goal** | Block obvious non-pothole content (people, indoor floors, manholes, walls, duplicate wide shot) while keeping real Indian road pothole photos |
| **Entry point** | `PotholePhotoValidator.validate()` |

**Production today:** rule-based **heuristics only** — no neural network runs in the photo gate. Optional on-device YOLO (`pothole_detect.tflite`) and cloud AI are planned but not active in the validator path.

---

## 2. Models used for classification

### 2.1 Active (production)

| Model / component | Type | Input | Role |
|-------------------|------|-------|------|
| `SceneHeuristics` | Pixel rules on 72×72 downsample | Bitmap | Road / asphalt / indoor / sky / circular vessel / road-line cues |
| `ContentClassifier` | Pixel rules on 72×72 downsample | Bitmap | Reject people, animals, plants, screens, saturated objects |
| `PotholeBlobLocator` | Flood-fill dark-blob finder (240 px edge) | Bitmap | Reject negative blob signatures (walls, pipes, garbage, etc.) |
| `RoadSceneHeuristics` | Perceptual hash (Hamming distance) | Wide + close-up bitmaps | Wide shot must differ from close-up |

**Implicit classes:** Accept vs Reject (not multi-class softmax).

**Source files:**

- `app/src/main/java/com/example/potholereport/ml/PotholePhotoValidator.kt`
- `app/src/main/java/com/example/potholereport/ml/SceneHeuristics.kt`
- `app/src/main/java/com/example/potholereport/ml/ContentClassifier.kt`
- `app/src/main/java/com/example/potholereport/ml/PotholeBlobLocator.kt`

### 2.2 Planned / optional (not active in validator today)

| Model | Asset | Runtime | Status |
|-------|-------|---------|--------|
| `PotholeDetector` | `assets/ml/pothole_detect.tflite` | LiteRT `Interpreter` | Code ready; **`.tflite` not bundled**; **not called** by validator |
| `CloudPotholeAnalysisClient` | Remote API | Network | Interface only — not implemented |

**Planned detector:** YOLOv8 detection or segmentation, class label `Pothole` (`pothole_labels.txt`), trained via `tools/train_pothole_model/train_yolov8.py`.

---

## 3. Independent features

Features computed **only from the photo** (and capture kind: close-up vs wide). No user input required.

### 3.1 Scene features (`SceneHeuristics`)

| Feature | Description |
|---------|-------------|
| `roadRatio` | Fraction of pixels classified as road-like |
| `asphaltRatio` | Dark asphalt / tar surface fraction |
| `indoorFloorRatio` | Indoor floor signature |
| `woodPanelRatio` | Wood / panel texture |
| `verticalStructureRatio` | Vertical structures (walls, poles) |
| `skyBandRatio` | Sky band in upper frame |
| `highSaturationRatio` | Highly saturated pixels |
| `circularVesselScore` | Round openings (bins, drain covers) |
| `horizontalRoadLineScore` | Horizontal road markings / tar lines |

### 3.2 Content features (`ContentClassifier`)

| Feature | Description |
|---------|-------------|
| `skinRatio` | Human skin tones |
| `vegetationRatio` | Grass, plants, trees |
| `animalFurRatio` | Animal fur texture |
| `foodOrObjectColorRatio` | Saturated food / object colours |
| `rectFrameLikelihood` | Photo-of-photo frame edges (disabled in practice) |
| `flatScreenLikelihood` | Phone / monitor screen signature |
| `saturatedNonRoadRatio` | Bright saturated non-road pixels |

### 3.3 Blob features (`PotholeBlobLocator`, full frame in validator)

| Feature | Description |
|---------|-------------|
| `areaPct` | Dark blob area as % of frame |
| `contrast` | Blob vs surrounding asphalt contrast |
| `blobInsideAsphaltFrac` | Fraction of blob overlapping asphalt |
| `aspect` | Blob width / height |
| `edgeTouches` | Whether blob touches frame edges |

Validator calls blob analysis with `position = null` (whole frame).

### 3.4 Wide-shot feature

| Feature | Description |
|---------|-------------|
| Perceptual hash Hamming distance | Distance between wide and close-up hashes — rejects duplicate / near-duplicate wide shots |

---

## 4. Dependent features

Features that require **user action**, **another photo**, or **upstream model output**. Used in risk analysis or future paths; only some apply to the classification gate.

| Feature | Depends on | Used in classification? |
|---------|------------|----------------------|
| Close-up URI | User must capture close-up first | **Yes** — wide validation needs verified close-up for hash check |
| `PotholePosition` (LEFT / MIDDLE / RIGHT) | User lane tile selection | **No** in validator; **yes** in `PotholeRiskAnalyzer` (lane-limited blob search) |
| `userSeverity` | User “How bad is it?” choice | **No** — risk scoring only |
| `PotholeDetection` / segmentation mask | `PotholeDetector` or cloud (future) | **No** today |
| Cloud classification result | `CloudPotholeAnalysisClient` (future) | **No** today |

Lane bands when position is used (risk analyzer):

- LEFT: 0–42% of frame width  
- MIDDLE: 30–70%  
- RIGHT: 58–100%

---

## 5. Classification thresholds

### 5.1 Close-up — scene & content

| Rule | Threshold | Component |
|------|-----------|-----------|
| Minimum road visible | `roadRatio` **≥ 0.13** | `PotholePhotoValidator` |
| Minimum asphalt or road lines | `asphaltRatio` **≥ 0.06** OR `horizontalRoadLineScore` **≥ 0.05** | same |
| Circular vessel (bin / drain) | `circularVesselScore` **≥ 0.62** AND `roadRatio` **< 0.42** | same |
| Indoor floor false positive | `indoorFloorRatio` **≥ 0.50** AND `roadRatio` **< 0.30** | `SceneHeuristics` |
| Person | `skinRatio` **≥ 0.45** | `ContentClassifier` |
| Vegetation | `vegetationRatio` **≥ 0.32** | same |
| Animal | `animalFurRatio` **≥ 0.55** | same |
| Phone / monitor screen | `flatScreenLikelihood` **≥ 0.62** | same |
| Object / food / sign | `foodOrObjectColorRatio` **≥ 0.30** AND `saturatedNonRoadRatio` **≥ 0.22** | same |
| Photo-of-photo frame | `rectFrameLikelihood` **≥ 1.05** | same (effectively **disabled**) |

### 5.2 Close-up — blob rejection

| Constant | Value | Rejects when |
|--------|-------|--------------|
| `BLOB_MAX_AREA_PCT` | **30.0** | Blob too large (wall, garbage pile) |
| `BLOB_MAX_CONTRAST` | **0.80** | Extreme contrast |
| `BLOB_MID_AREA_PCT` + `BLOB_MID_INSIDE_ASPHALT` | **18.0** / **0.50** | Large blob mostly off asphalt (cement pipe, wall) |
| `BLOB_HARD_INSIDE_ASPHALT` + area floor | **0.18** / **4%** | Small blob not on asphalt |

### 5.3 Wide shot

| Rule | Threshold |
|------|-----------|
| Minimum road visible | `roadRatio` **≥ 0.24** |
| Must differ from close-up | Perceptual hash Hamming distance **≥ 8** (reject if **< 8**) |
| Outdoor Indian road composite | `looksLikeOutdoorIndianRoad` rules in `SceneHeuristics` |

### 5.4 Planned neural detector (when shipped)

| Constant | Value |
|----------|-------|
| `YOLO_MIN_SCORE` | **0.18** |
| Segmentation mask binarize | **0.5** |

Full change log: [`tools/train_pothole_model/CALIBRATION_NOTES.md`](../tools/train_pothole_model/CALIBRATION_NOTES.md)

---

## 6. How classification accuracy is tested

### 6.1 Calibration datasets

| Set | Size | Label meaning |
|-----|------|---------------|
| Positive close-ups | **37 photos** (34 unique) | Real Indian road potholes — should **Accept** |
| Negative samples | **53 photos**, 7 categories | Non-potholes — should **Reject** |

Negative categories: cement pipelines (17), garbage (2), manhole (8), road blockers (2), sign board (3), trees (6), wall boundaries (15).

Paths: `tools/train_pothole_model/dataset/calibration_close_up/` and `.../Pothole Negative samples/`

### 6.2 Measured results (heuristic validator, Round 2)

After blob rules were added (`CALIBRATION_NOTES.md`, May 2026):

| Outcome | Before blob rules | After blob rules |
|---------|-------------------|------------------|
| Negatives rejected | 3 / 53 (**6%**) | 28 / 53 (**53%**) |
| Positives kept | 37 / 37 (**100%**) | 33 / 34 (**97%**) |

**Per-category negative catch rate (after):** Cement 14/17 · Garbage 2/2 · Manhole 2/8 · Road blockers 2/2 · Sign board 0/3 · Trees 4/6 · Wall 4/15

**Tuning policy:** conservative — prefer letting a borderline negative through over rejecting a real pothole (manholes and walls overlap pothole blob signatures).

### 6.3 Metrics used

#### Heuristic classifier (current production)

| Metric | Used? | Notes |
|--------|-------|-------|
| **Accuracy** | **No** (not computed formally) | Pass/reject counts on fixed photo sets only |
| **Precision** | **No** | Not in calibration tooling |
| **Recall** | **No** | Not in calibration tooling |
| **F1** | **No** | Not computed |
| **Confusion matrix** | **No** (not auto-generated) | Can be derived offline from CSV outputs (TP/TN/FP/FN on Accept/Reject) |

**Offline calibration tools** (`tools/calibrate/`):

| Tool | Output CSV | Purpose |
|------|------------|---------|
| `CalibrateNegatives.java` | `negatives_report.csv` | Per-category false-accept rate |
| `BlobSurvey.java` | `blob_survey.csv` | Blob rule grid search vs positives/negatives |
| `Calibrate.java` | `calibration_report.csv` | Scene/content threshold sweep (Round 1) |

#### Planned YOLOv8 detector (when trained)

| Metric | Used? | Target |
|--------|-------|--------|
| **mAP@0.5** | **Yes** | **≥ 0.70** production minimum; **≥ 0.85** ideal |
| **mAP@0.5:0.95** | **Yes** | COCO-style summary in Ultralytics logs |
| **Precision** | **Yes** | Per-class in Ultralytics validation |
| **Recall** | **Yes** | Per-class in Ultralytics validation |
| **Confusion matrix** | **Yes** | Ultralytics plots in `output/runs/pothole/` |

Export: `tools/train_pothole_model/train_yolov8.py` → `output/training_metrics.json`

**Repo state:** placeholder training labels only; no committed `.tflite` or `training_metrics.json` yet.

### 6.4 Confusion matrix (how it maps to this task)

For the binary photo gate:

|  | Predicted Accept | Predicted Reject |
|--|------------------|------------------|
| **Actual pothole** | True Positive (TP) | False Negative (FN) |
| **Actual non-pothole** | False Positive (FP) | True Negative (TN) |

From Round 2 numbers on 34 unique positives + 53 negatives:

- TP ≈ 33, FN ≈ 1  
- TN ≈ 28, FP ≈ 25  

These are **empirical calibration counts**, not metrics logged in the app or CI. A formal confusion matrix script is planned (see backlog in `ml_models_and_evaluation.md` §8).

### 6.5 Field / manual testing

- Real potholes, manholes, bins, indoor floors, duplicate wide/close pairs  
- Smoke checklist: [`release_and_versioning_guide.md`](release_and_versioning_guide.md) §5.2

---

## 7. Quick answers

| Question | Answer |
|----------|--------|
| What classifies potholes today? | Heuristic rules in `PotholePhotoValidator` — **not** TFLite/YOLO |
| Independent features? | Scene, content, blob, and wide-hash signals (§3) |
| Dependent features? | Close-up URI for wide check; lane/severity for risk only (§4) |
| Is **accuracy** checked? | **No** single score in app; calibration uses pass/reject rates |
| Is **precision / recall** checked? | **No** for heuristics; **yes** for future YOLO training |
| Is **confusion matrix** used? | **Not automated** today; derivable offline; Ultralytics when YOLO ships |

---

## 8. Related documentation

| Document | Content |
|----------|---------|
| [`ml_models_and_evaluation.md`](ml_models_and_evaluation.md) | Full ML pipeline, risk analyzer, depth, cloud |
| [`product_assumptions.md`](product_assumptions.md) | GPS, duplicate radius, app defaults |
| [`tools/train_pothole_model/CALIBRATION_NOTES.md`](../tools/train_pothole_model/CALIBRATION_NOTES.md) | Threshold change history |
| [`tools/train_pothole_model/README.md`](../tools/train_pothole_model/README.md) | YOLO training workflow |

---

*When classification thresholds or models change, update this file, `ml_models_and_evaluation.md`, and `CALIBRATION_NOTES.md` together.*
