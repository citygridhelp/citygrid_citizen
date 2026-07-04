# City Grid citizen app — product & technical assumptions

Central reference for **defaults, thresholds, and design decisions** baked into the citizen app (`potholereport`). Values below are taken from the current codebase unless marked *planned* (backlog only, not shipped).

Related docs:

- [future_features_backlog.md](future_features_backlog.md) — enhancements that may change these assumptions
- [future_govt_bbmp_gba_zones.md](future_govt_bbmp_gba_zones.md) — Bengaluru municipal boundaries (BBMP / GBA)
- [release_and_versioning_guide.md](release_and_versioning_guide.md) — release process
- [`pothole_classification_model.md`](pothole_classification_model.md) — pothole photo classification: models, features, thresholds, metrics
- [`ml_models_and_evaluation.md`](ml_models_and_evaluation.md) — full ML pipeline, risk analyzer, evaluation
- `tools/train_pothole_model/CALIBRATION_NOTES.md` — ML / photo-validation thresholds (detailed)

*Last reviewed: July 2026 · app `in.citygrid.citizen` · `versionName` 1.0*

---

## 1. Reporting the same pothole (duplicate guard)

**Rule:** One reporter may submit **only one active report** for the same physical pothole until the earlier report is **Completed**.

| Assumption | Value | Source |
|------------|-------|--------|
| Same-pothole radius | **40 m** | `PotholeDuplicateGuard.SAME_POTHOLE_RADIUS_METERS` |
| Rationale (in code) | ~40 m covers same pothole + GPS jitter; avoids blocking the **opposite lane** | Comment in `PotholeDuplicateGuard.kt` |
| Scope | Same `reporterUserId` + same `cityKey` + distance ≤ 40 m | `findActiveDuplicate` |
| Blocking statuses | **Open**, **In progress** block a new report | `blocksNewReportAtSamePlace()` |
| Allowed after | **Completed** — user may report again at same coordinates | `PotholeDuplicateGuard.kt` |
| User message | “You already reported this pothole here (status)…” | `NewReportScreen.kt` |

**Not assumed today:** Server-side duplicate enforcement (guard is **client + local cache** before push). Metro/city boundary check on submit is *planned* (backlog §5), not enforced yet.

---

## 2. GPS & report location

| Assumption | Value | Source |
|------------|-------|--------|
| Location required to submit | Yes — lat/lng must be available (`readyToSubmit`) | `NewReportScreen.kt` |
| GPS providers tried (report screen) | Last-known from GPS, network, passive; then fresh GPS (**6.5 s** timeout), then network (**3.5 s**) | `fetchBestCalibratedLocation` |
| Location quality score | `accuracy (m) + ageSeconds × 0.35` — lower is better; age capped at **180 s** | `locationQualityScore` |
| Map home GPS (similar pipeline) | GPS timeout **6500 ms**, network **3000 ms**, passive fallback **2800 ms** | `HomeScreen.kt` |
| Accuracy shown on home map | `GPS accuracy: ±Xm` or “searching…” when fix has no accuracy | `HomeScreen.kt` |
| Manual location override | User may paste Maps URL / lat,lng on report screen | `parseMapsInput` |
| Max accuracy for submit | **None enforced** — poor GPS can still submit (*improvement planned* backlog §12, e.g. warn/block > 50 m) |

---

## 3. Report content & submit

| Assumption | Value | Source |
|------------|-------|--------|
| Photos required | **Two**: close-up + wide road | `NewReportScreen.kt` |
| On-device photo check | TensorFlow Lite / LiteRT validators before submit enabled | UI copy + `PotholePhotoValidator` |
| Note max length | **240** characters | `noteMax` in `NewReportScreen.kt` |
| Default severity | **Moderate** | `selectedSeverity` initial state |
| Severity levels | Minor, Moderate, Severe, Critical | `PotholeSeverity.kt` |
| Lane / position | User selects position (affects ML risk path) | `PotholePosition`, `NewReportScreen.kt` |
| Cloud upload | Only when **signed in** + Supabase configured; else local save + message | `NewReportScreen` submit branch |
| Submit loading UI | **None** — button stays “SUBMIT REPORT” during IO (*planned* §13) |

---

## 4. Map — clusters, filters, labels

| Assumption | Value | Source |
|------------|-------|--------|
| Reports on map | Same `cityKey` and coordinates **inside city metro bbox** | `RecentReportsRepository.reportsForMapInMetro` |
| Normal map clustering | **6×6 grid** over metro bbox; count = reports per cell | `buildReportGridClusters` |
| Active critical clustering | **500 m** radius merge | `ACTIVE_CRITICAL_CLUSTER_RADIUS_METERS` |
| Critical filter | Only **Open / In progress** + **Critical** severity | `attachReportClusterOverlay` |
| Cluster tap | **No action** — display only (*planned* §19) |
| Pan/zoom vs critical mode | Pan/zoom **clears** critical filter today (*bug* — fix planned §16) |
| Region labels (e.g. Bengaluru) | ~200 localities; each has **minZoom** — labels appear only when zoomed in enough | `bengaluruRegionLabelSpecs` |
| City view vs street view | City label at min zoom band; `CITY_VIEW_ZOOM_EPSILON` controls “at city zoom” | `HomeScreen.kt` |
| Default selected city | **BENGALURU** | `HomeScreen`, `CityMetroKeys.NAV_FALLBACK_CITY` |
| Metro bbox example (Bengaluru) | N 13.16, E 77.92, S 12.63, W 77.33 | `cityMetroBounds` |

---

## 5. Recent reports & local storage

| Assumption | Value | Source |
|------------|-------|--------|
| Max reports stored locally | **80** | `RecentReportsRepository.MAX_STORED` |
| Recent strip per city | **5** newest (in metro bbox when coords exist) | `recentForCityInMetro(..., limit = 5)` |
| Public city sync fetch limit | **80** rows from Supabase | `ReportSyncRepository.fetchRecentCityReports` |
| My Reports scope | Signed-in: rows where `reporter_auth_id` = auth user | `fetchMyReports` |
| Guest map feed | Public city sync still runs when configured | `AppAutoRefresh.refreshLocalData` |

---

## 6. Identity, auth & privacy

| Assumption | Value | Source |
|------------|-------|--------|
| Reporter public id format | **`PW-` + 8** chars (A–Z, 2–9, no ambiguous I/O/0/1) | `UserProfileRepository.generateAnonymousUserId` |
| One email = one account | No account linking; duplicate signup blocked via `email_exists` RPC | Backlog §1, `SupabaseAuthRepository` |
| Signup password (full screen) | Min **6** characters | `SignupScreen.kt` |
| Signup password (report modal) | Min **6** + upper + lower + digit + symbol | `ReportSignInModal.isStrongCitizenPassword` |
| Login password | Min **6** characters | `LoginScreen.kt` |
| Signup verification | Email OTP via Supabase; **4+** digit code field on verify | `SignupScreen.kt` |
| Report flow without sign-in | Home **Report a pothole** opens **sign-in modal** for guests | `HomeScreen.kt` |
| Anonymous on public map | Reporter id / email not shown on shared map views | Product design; `AccountabilitySection` copy |
| Device id (unsigned) | `DeviceReporterId` for local reporter id when opening report screen without session | `AuthNavHost.kt` |

---

## 7. Municipal accountability routing

| Assumption | Value | Source |
|------------|-------|--------|
| Assignee selection | **Nearest zone centroid** by GPS within city | `MunicipalAssignmentResolver` → `MunicipalContactsRegistry.nearestAssignee` |
| Bengaluru zones modeled | **8 BBMP zones** (East, West, South, Mahadevapura, Bommanahalli, RR Nagar, Dasarahalli, Yelahanka) | `MunicipalContactsRegistry.bbmpZones()` |
| GBA 5 corporations / 369 wards | **Not** used for routing yet | `future_govt_bbmp_gba_zones.md` |
| Fallback | City HQ assignee if GPS invalid or no zone match | `fallbackForCity` |
| Officer data | Public directory snapshots in code + `supabase/seed/officers.json` — **re-verify before production** | Comments in registry |

---

## 8. Sync, notifications & app lifecycle

| Assumption | Value | Source |
|------------|-------|--------|
| Evidence storage bucket | **`evidence`** (Supabase Storage) | `ReportSyncRepository` |
| Push survives screen close | Upload on app-level `CoroutineScope`, not composition scope | `enqueuePush` |
| Splash minimum time | **1.5 s** on cold start | `AppAutoRefresh.SPLASH_MIN_MS` |
| Background refresh trigger | **10 minutes** idle | `AppAutoRefresh.RESUME_IDLE_MS` |
| In-app notifications cap | **50** items | `CitizenNotificationsRepository.MAX_PILE` |
| App update notice | Local compare of `versionName` vs last seen | `CitizenNotificationsRepository.checkAppVersion` |
| Confirmation email ticket id | **`CG-` + last 8** hex chars of `reports.id` | `report_confirmation_email.md` |
| Confirmation email time | **IST** | `report_confirmation_email.md` |

---

## 9. ML & photo validation (summary)

Detailed numbers live in **`tools/train_pothole_model/CALIBRATION_NOTES.md`**. High-level assumptions:

| Assumption | Value | Source |
|------------|-------|--------|
| Close-up min road ratio | **0.13** | `PotholePhotoValidator` |
| Wide shot min road ratio | **0.24** | `PotholePhotoValidator` |
| Duplicate photo hash distance | Reject if perceptual hash distance **< 8** | `MIN_HASH_DISTANCE` |
| YOLO detection min score | **0.18** | `PotholeDetector` |
| Risk / blob thresholds | Calibration-derived; lane-aware path stricter than pre-submit validator | `CALIBRATION_NOTES.md` |

---

## 10. Platform & release identifiers

| Assumption | Value | Source |
|------------|-------|--------|
| Application id (Play Store) | **`in.citygrid.citizen`** | `app/build.gradle.kts` |
| Min SDK | **24** | `build.gradle.kts` |
| Target SDK | **36** | `build.gradle.kts` |
| Release minify (R8) | **Off** | `isMinifyEnabled = false` |
| Supabase keys | `local.properties` only — not in git | `build.gradle.kts` |

---

## 11. Assumptions *not* enforced yet (backlog / planned)

These are **documented intentions**, not current behaviour:

| Topic | Planned assumption | Backlog |
|-------|-------------------|---------|
| City/metro boundary on submit | Reject or warn if GPS outside selected city metro | §5 |
| GPS accuracy gate | Warn or block submit above ~**50 m** accuracy | §12 |
| Signup OTP interrupt | Resume / resend for unconfirmed email | §11 |
| GBA heat map | Red gradient by report count per division **inside city only** | §17 |
| Map cluster tap | Open report list / detail from cluster number | §19 |

---

## 12. How to update this file

When you change a constant or product rule in code:

1. Update the table row here in the same PR/commit.
2. If the change is user-visible, note it in release notes / `versionName` bump.
3. For ML threshold changes, update **`CALIBRATION_NOTES.md`** and **[`ml_models_and_evaluation.md`](ml_models_and_evaluation.md)** first, then summarize here.

When an assumption is debated (e.g. duplicate radius 40 m → 30 m), record **why** in a one-line rationale column or a short note under the section.
