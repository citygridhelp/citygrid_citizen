# City Grid citizen app — product & technical assumptions

Central reference for **defaults, thresholds, and design decisions** baked into the citizen app (`in.citygrid.citizen` / package `potholereport`). Values below are taken from the current codebase unless marked *planned* (backlog only, not shipped).

Related docs:

- [future_features_backlog.md](future_features_backlog.md) — enhancements that may change these assumptions
- [future_govt_bbmp_gba_zones.md](future_govt_bbmp_gba_zones.md) — Bengaluru municipal boundaries (BBMP / GBA)
- [release_and_versioning_guide.md](release_and_versioning_guide.md) — release process
- [`pothole_classification_model.md`](pothole_classification_model.md) — pothole photo classification: models, features, thresholds, metrics
- [`ml_models_and_evaluation.md`](ml_models_and_evaluation.md) — full ML pipeline, risk analyzer, evaluation
- `tools/train_pothole_model/CALIBRATION_NOTES.md` — ML / photo-validation thresholds (detailed)
- [`../tools/train_pothole_model/MODEL_UPDATE_PROCESS.md`](../tools/train_pothole_model/MODEL_UPDATE_PROCESS.md) — #33M sample targets + retrain cadence

*Last reviewed: 25 July 2026 · app `in.citygrid.citizen`*

---

## 1. Reporting the same pothole (duplicate guard)

**Rule:** One reporter may submit **only one active report** for the same physical pothole until the earlier report is **Completed**.

| Assumption | Value | Source |
|------------|-------|--------|
| Same-pothole radius | **30 m** (field-tunable; was 25 m then 40 m) | `PotholeDuplicateGuard.SAME_POTHOLE_RADIUS_METERS` |
| Effective radius | `max(30 m, GPS accuracy × 1.25)` | `effectiveRadiusMeters` |
| Rationale | Covers same pothole + GPS jitter; avoid blocking a distinct nearby hole or the opposite lane | Comments / Phase F |
| Scope | Same `reporterUserId` + same `cityKey` + distance ≤ effective radius | `findActiveDuplicate` |
| Blocking statuses | **Open**, **In progress** block a new report | `blocksNewReportAtSamePlace()` |
| Allowed after | **Completed** — user may report again at same coordinates | `PotholeDuplicateGuard.kt` |
| User message | “You already reported this pothole here (status)…” | `NewReportScreen.kt` |

**Not assumed today:** Server-side duplicate enforcement (guard is **client + local cache** before push). Metro/city boundary check on submit is *planned* (backlog), not enforced yet.

---

## 2. GPS & report location

| Assumption | Value | Source |
|------------|-------|--------|
| Location required to submit | Yes — lat/lng must be available (`readyToSubmit`) | `NewReportScreen.kt` |
| GPS providers tried (report screen) | Last-known from GPS, network, passive; then fresh GPS (**6.5 s** timeout), then network (**3.5 s**) | `fetchBestCalibratedLocation` |
| Location quality score | `accuracy (m) + ageSeconds × 0.35` — lower is better; age capped at **180 s** | `locationQualityScore` |
| Map home GPS (similar pipeline) | GPS timeout **6500 ms**, network **3000 ms**, passive fallback **2800 ms** | `HomeScreen.kt` |
| Accuracy shown on home map | Overlay: `GPS ±Xm` or `GPS searching…` | `HomeScreen.kt` (`OsmDensityMap`) |
| Manual location override | User may paste Maps URL / lat,lng on report screen | `parseMapsInput` |
| Max accuracy for submit | **Warn** if GPS > **50 m**; **block** auto-GPS submit if > **100 m** (manual coords bypass) | `NewReportScreen.kt` |
| Accuracy on report screen | `GPS ±Xm` with green / amber / red by threshold | `LocationBlock` in `NewReportScreen.kt` |

### Trip / navigation GPS accept rules

| Assumption | Value | Source |
|------------|-------|--------|
| Reject coarse fixes | GPS accuracy > **40 m**; network > **25 m** | `shouldAcceptNavigationLocation` |
| Prefer fresh GPS | Stale / implausible jumps rejected | `HomeScreen.kt` |
| Lane side label on alerts | Only when GPS accuracy ≤ **25 m** | `TripNavigationMatcher.SIDE_MIN_ACCURACY_M` |

---

## 3. Report content & submit

| Assumption | Value | Source |
|------------|-------|--------|
| Photos required | **Two**: close-up + wide road | `NewReportScreen.kt` |
| On-device photo check | TensorFlow Lite / LiteRT validators before submit enabled | UI copy + `PotholePhotoValidator` |
| Note max length | **240** characters | `noteMax` in `NewReportScreen.kt` |
| Default severity | **Moderate** | `selectedSeverity` initial state |
| Severity levels | Minor, Moderate, Severe, Critical | `PotholeSeverity.kt` |
| Lane / position | **L / M / R / LM / MR / LMR** on wide-shot confirm (contiguous only; no LR). **LMR** = full road (legacy `F` still accepted). Section 04 removed | `PotholePosition`, `PotholeLaneSpan`, `WideShotLaneConfirmDialog` |
| Wide-shot soft align | Warn when guideline match &lt; **~85%** (soft gate, not hard block) | `WideShotAlignmentGate`, `WIDE_SHOT_ALIGN_SOFT_TARGET` |
| Traffic Flip | Default arrows toward camera = facing; **Flip** = against traffic | `TrafficFacingMode` via confirm dialog |
| Direction metadata | `pothole_position`, `report_bearing_deg`, `traffic_bearing_deg`, `traffic_faces_camera` | migration `0012` |
| Cloud upload | Only when **signed in** + Supabase configured; else local save + message | `NewReportScreen` submit branch |

---

## 4. Map — clusters, filters, labels

| Assumption | Value | Source |
|------------|-------|--------|
| Reports on map | Same `cityKey` and coordinates **inside city boundary** (GBA polygon for Bengaluru; bbox for others) | `RecentReportsRepository.reportsForMapInMetro` |
| Normal map clustering | Screen-overlap merge at current zoom | `buildReportScreenOverlapClusters` |
| Map severity filter | Dropdown: All / Minor / Moderate / Severe / Critical | `HomeScreen.kt` |
| Region labels (Bengaluru) | Curated locality points; G-lite tiering | `RegionNameLabelsOverlay` |
| Basemap | CARTO **light_nolabels** — no tile place names (by design) | `CartoPositronNoLabels` |
| Default selected city | **BENGALURU** | `CityMetroKeys.NAV_FALLBACK_CITY` |
| Metro bbox (Bengaluru) | Official **GBA** outer boundary (Sept 2025) | `BengaluruGbaBoundary.kt` |

---

## 5. Trip navigation & alternate routes

**Product intent:** Show **usable, connected** ways to the same destination so the user can compare and pick. Do **not** invent disconnected or pointless paths. Slightly slower options are kept (Bengaluru traffic can make a few minutes irrelevant). There is **no hard “only 3 routes”** product rule — soft UI/probe cap only.

### 5.1 Modes & engine

| Assumption | Value | Source |
|------------|-------|--------|
| Modes | **Car**, **Bike**, **Walk** | `TripVehicle` |
| Route engine | FOSSGIS public OSRM `routing.openstreetmap.de` (`routed-car` / `routed-bike` / `routed-foot`) | `OsrmRouteClient` |
| Live traffic | **Not used yet** — no congestion overlay. **Journey-start ETA** uses hour-of-week factors (`JourneyStartEta`) vs free-flow OSRM (“Leave now ~X · Usually Y”). Mappls/TomTom live feed still planned under #31 Phase B | Product decision Jul–Aug 2026 |
| Request throttle | ≥ **~400 ms** between OSRM calls (public host courtesy) | `throttledGet` |
| Start/end snap radius | **500 m** (courtyard/park → nearest road) | `SNAP_RADIUS_M` |
| Soft max routes shown | **3** (1 primary + up to 2 useful alts — #23.1 trust over quantity) | `MAX_ROUTE_OPTIONS` |
| Native OSRM alternatives | Request at most **3** — FOSSGIS returns `TooBig` (zero routes) if higher | `OSRM_MAX_ALTERNATIVES` |

### 5.2 Bike road-network fallback

OSM cycle graphs in Indian cities are often sparse/disconnected. Bike may use **car road geometry** with a bike-speed ETA.

| Assumption | Value | Source |
|------------|-------|--------|
| Prefer bike graph when | Not much longer than car **and** snaps are reasonable | `pickFeasibleBikeRoute` |
| Fall back to roads when | Bike distance > car × **1.35** and > car + **600 m**, **or** start/end snap ≥ **90 m** and worse than car by **> 40 m** | `BIKE_DETOUR_*`, `BIKE_BAD_SNAP_M` |
| Bike ETA on road geometry | ~**16 km/h** (`distance / 4.5` m/s), min **60 s** | `asBikeEta` |
| Alternate routes when fallback is on | **All** diversions stay on **driving** network + bike ETA — no fragile cycle-only scraps next to a road primary | `useRoadNetwork` / `fetchDivertedRoute` |

### 5.3 Discovering alternate corridors

When OSRM returns one path (common), the app probes left/right **via** points along the primary.

| Assumption | Value | Source |
|------------|-------|--------|
| Short trips (< ~2 km) | Via offsets ~**55–220 m** (parallel streets) | `diversionViaPoints` |
| Mid trips | Offsets ~**150–700 m** | `diversionViaPoints` |
| Long trips (≥ ~5 km) | Corridor offsets ~**400–2200 m** (e.g. Mahadevapura vs ORR-style) | `diversionViaPoints` |
| Via snap (middle waypoint) | **55 m** short / **120 m** long trips — must stay tight so via does not snap back onto the primary road | `fetchViaRoute` |
| Diversity | Prefer routes with larger mid-path geographic gap; drop near-duplicates | `pickDiverseRoutes`, `routesAreVisiblyDistinct` |

### 5.4 Hard quality gates (reject before draw)

**Do not** reject merely for being a few minutes slower. Reject if:

| Gate | Rule (summary) | Why |
|------|----------------|-----|
| Start/end join | Candidate start/end must be within ~**primary join + 100 m** (floor ~**140 m**) of the requested points | Connectivity (no “floating” spur at origin) |
| Snap quality | Start/end snap not much worse than primary (+**120 m** and absolute > **140 m**) | Same |
| Extreme length | Distance > primary × **1.55** + **1200 m** | Pointless mega-detour |
| Extreme time | Duration > primary × **1.50** + **10 min** | e.g. 49 min vs 24 min waste |
| Geometry (via-probed only) | True out-and-back (path/chord ≥ ~5) or gross corridor deviation (≥ ~2.5–3.5 km wider than primary) | Waste spurs / wrong-way loops |
| Native OSRM alts | Light filter only (connectivity + time/distance band) | Engine-ranked; don’t over-filter |
| Soft fallback | If strict via filter leaves &lt; 2 routes, keep light-filtered distinct corridors | Always try to show a real choice |
| Excess U-turns (strict) | ≥ **4** sharp reversals and **> primary + 2** | Messy path |

| Assumption | Value | Source |
|------------|-------|--------|
| Quality API | `passesRouteQuality`, `hasPointlessSpur`, `countSharpReversals` | `OsrmRouteClient.kt` |
| User choice | Survivors are shown with time + distance bubbles; user taps to select before Navigate | `TripNavigationOverlay`, `HomeScreen` |

### 5.5 Map preview UI

| Assumption | Value | Source |
|------------|-------|--------|
| Selected route | Blue polyline | `TripNavigationOverlay` |
| Alternate routes | Orange polylines | `setPreviewAlternates` |
| ETA bubbles | Time + distance; tip anchored **on** the route; positions spaced on-screen (~**150 dp**) to avoid overlap | `pickSpacedLabelPoints` |
| Trip vs Heat | **Mutually exclusive** — Heat off while Trip is on | `HomeScreen.kt` |
| Report from trip map | Report control available in trip mode (overlay; do not tear down MapView on open) | Field-fix / UI fix |

### 5.6 Along-route pothole matching

| Assumption | Value | Source |
|------------|-------|--------|
| Ahead cone (no route) | Half-angle **35°**, max **2500 m**, min **5 m** | `TripNavigationMatcher` |
| Route corridor | Perpendicular ≤ **28 m** from polyline | `ROUTE_CORRIDOR_M` |
| Max alert distance | **2500 m** ahead (overview); street-follow map shows closer subset | `MAX_AHEAD_M`, street-follow caps |
| Off-route trigger | Deviates **> 60 m** from route (accuracy-aware) | `OFF_ROUTE_REROUTE_M` |
| Deviation recovery (#23.3) | Dest-reaching known alt → **auto-adopt**; corridor that **misses dest** → dashed **rejoin**; wrong network for vehicle (e.g. car on trail) → **U-turn** + rematch; else **full reroute** on vehicle profile | `TripNavigationMatcher.planDeviation` |
| Pothole-aware ranking (#23.2) | Within **~120 s** ETA of each other, prefer lower severity-weighted risk (1/2/4/6); chip **“Similar time · fewer severe potholes”** | `TripRoutePotholeRanking` |
| Covered route display | Traveled segment **hidden** — only remaining polyline drawn | `TripNavigationOverlay`, MapLibre trip route |
| Route prefetch | OSRM + #23.2 ranking warm as soon as origin+destination known (search/map tap); Direction/Navigation reuse cache; successful packs persist in [TripRouteOfflineCache] for offline Direction | `tripRoutePrefetch`, `TripRouteOfflineCache`, `HomeScreen` |
| Start correction | If start was off by ≥ **15 m** (or accuracy-scaled), correct from live GPS | `START_CORRECTION_M` |
| Pothole notify distance | Voice/notify within **150 m** | `TRIP_POTHOLE_NOTIFY_WITHIN_M` |
| Edge danger blink (#27) | Severe/Critical only; speed-scaled start **150 / 200 / 250 m** (&lt;30 / 30–69 / ≥70 km/h); cluster merge gap **≤ 7 m**; stronger pulse on **L/R/S**; Critical harder/faster; blink **on when muted**; stop after hole/**cluster end** passed | `TripDangerBlink`, `TripDangerEdgeBlink` |
| Follow zoom | OSM street **20.5 / 21.2 / 22.0**; MapLibre street **16.8 / 17.6 / 18.4** (slow/mid/fast); look-ahead up to **~18 m** when moving; nav overview ~**15.5**; OSM trip max zoom **22** | `TripStreetCamera`, `TRIP_FOLLOW_ZOOM`, `TRIP_MAP_MAX_ZOOM` |
| Offline nav | GPS + dead-reckon continue; **Direction offline** when destination was already routed (durable cache) even if start/GPS updates; no full OSRM reroute offline; known-alt / rejoin still work; offline chip in nav chrome; **on reconnect** refresh tiles + quiet mid-nav reroute **and** re-fetch planner routes | `TripRouteOfflineCache`, `NetworkReachability`, `HomeScreen`, `MapLibreDensityMap` |
| Map chrome scale | FABs / compass / GPS chip / planner / nav rail scale with screen width (**0.84–1.08** vs 360dp); alt chips single-line | `MapUiScale`, `rememberMapUiScale` |

### 5.7 Place search / labels

| Assumption | Value | Source |
|------------|-------|--------|
| Search bias | City metro bbox + city-scoped geocode; Photon fuzzy autocomplete; Overpass landmarks (fuel, hospital, locality, PG, …) — all clickable | `TripPlaceSearch`, `TripPlaceSearchRemote`, `HomeScreen` |
| Labels | Prefer place/institution name over house number / “Building 41”; may fall back to query text | `TripPlaceLabels` |
| Recents | Cap destinations / starts / favorites; matching recents appear in typed suggestions | `TripRecentPlacesRepository` |

---

## 6. Recent reports & local storage

| Assumption | Value | Source |
|------------|-------|--------|
| Max reports stored locally | **80** | `RecentReportsRepository.MAX_STORED` |
| Recent strip per city | **5** newest (in metro bbox when coords exist) | `recentForCityInMetro(..., limit = 5)` |
| Public city sync fetch limit | **80** rows from Supabase | `ReportSyncRepository.fetchRecentCityReports` |
| My Reports scope | Signed-in: rows where `reporter_auth_id` = auth user | `fetchMyReports` |
| Guest map feed | Public city sync still runs when configured | `AppAutoRefresh.refreshLocalData` |

---

## 7. Identity, auth & privacy

| Assumption | Value | Source |
|------------|-------|--------|
| Reporter public id format | **`PW-` + 8** chars (A–Z, 2–9, no ambiguous I/O/0/1) | `UserProfileRepository.generateAnonymousUserId` |
| One email = one account | No account linking; duplicate signup blocked via `email_exists` RPC | `SupabaseAuthRepository` |
| Signup / login password | Min **6** characters (report modal may require stronger) | Auth screens |
| Report flow without sign-in | Guests prompted to sign in before report | `HomeScreen.kt` |
| Anonymous on public map | Reporter id / email not shown on shared map views | Product design |
| Device id (unsigned) | `DeviceReporterId` for local reporter id when no session | `AuthNavHost.kt` |

---

## 8. Municipal accountability routing

| Assumption | Value | Source |
|------------|-------|--------|
| Assignee selection | **GBA ward polygon** → corporation assignee (Bengaluru); legacy BBMP zone lookup for old reports | `BengaluruMunicipalRouting` |
| Bengaluru zones modeled | **5 GBA corporations** + **369 wards** at submit | `MunicipalContactsRegistry`, `bengaluru_gba_wards.json` |
| Fallback | City HQ assignee if GPS invalid or no zone match | `fallbackForCity` |
| Officer data | Public directory snapshots — **re-verify before production** | Registry comments |

---

## 9. Sync, notifications & app lifecycle

| Assumption | Value | Source |
|------------|-------|--------|
| Evidence storage bucket | **`evidence`** (Supabase Storage) | `ReportSyncRepository` |
| Splash minimum time | **1.5 s** on cold start | `AppAutoRefresh.SPLASH_MIN_MS` |
| Background refresh trigger | **10 minutes** idle | `AppAutoRefresh.RESUME_IDLE_MS` |
| In-app notifications cap | **50** items | `CitizenNotificationsRepository.MAX_PILE` |
| Confirmation email ticket id | **`CG-` + last 8** hex chars of `reports.id` | `report_confirmation_email.md` |
| Confirmation email time | **IST** | `report_confirmation_email.md` |

---

## 10. ML & photo validation (summary)

Detailed numbers live in **`tools/train_pothole_model/CALIBRATION_NOTES.md`**. High-level:

| Assumption | Value | Source |
|------------|-------|--------|
| Close-up min road ratio | **0.13** | `PotholePhotoValidator` |
| Wide shot min road ratio | **0.24** | `PotholePhotoValidator` |
| Duplicate photo hash distance | Reject if perceptual hash distance **< 8** | `MIN_HASH_DISTANCE` |
| YOLO detection min score | **0.18** | `PotholeDetector` |
| YOLO retrain cadence | After **+50–100** new verified boxed photos, or monthly | #33M / `MODEL_UPDATE_PROCESS.md` |

---

## 11. Platform & release identifiers

| Assumption | Value | Source |
|------------|-------|--------|
| Application id (Play Store) | **`in.citygrid.citizen`** | `app/build.gradle.kts` |
| Min SDK | **24** | `build.gradle.kts` |
| Target SDK | **36** | `build.gradle.kts` |
| Release minify (R8) | **Off** | `isMinifyEnabled = false` |
| Supabase keys | `local.properties` only — not in git | `build.gradle.kts` |

---

## 12. Assumptions *not* enforced yet (backlog / planned)

| Topic | Planned assumption |
|-------|-------------------|
| Live traffic in routing | Rank / label routes by congestion |
| City/metro boundary on submit | Reject or warn if GPS outside selected city metro |
| Server-side duplicate guard | Enforce 30 m rule in Supabase/RPC |
| Map cluster tap | Open report list / detail from cluster |
| Edge danger blink (#27) | **Shipped in app** — see §5.6 |
| Bilingual map labels (#25.3) | English + regional (GPS-default, user-selectable) via name overlay |
| Turn-aware pothole alerts (#29) | Order vs turn + speed-scaled repeats; coalesce when close |

---

## 13. How to update this file

When you change a constant or product rule in code:

1. Update the matching row here in the same PR/commit.
2. If the change is user-visible, note it in release notes / `versionName` bump.
3. For ML threshold changes, update **`CALIBRATION_NOTES.md`** and **[`ml_models_and_evaluation.md`](ml_models_and_evaluation.md)** first, then summarize here.
4. For trip-routing quality gates or OSRM hosts, update **§5** and the unit tests in `OsrmRouteAlternatesTest.kt` / `OsrmBikeFallbackTest.kt`.

When an assumption is debated (e.g. duplicate radius, route soft max), record **why** in a one-line rationale or a short note under the section.
