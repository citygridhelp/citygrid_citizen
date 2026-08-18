# Citizen app — future features backlog

**This is the master enhancement list** for the City Grid citizen app (`potholereport`).

| Role | File |
|------|------|
| **Master list (update after every implementation)** | **`docs/future_features_backlog.md`** ← this file |
| GBA / BBMP detail (§10 only) | [`docs/future_govt_bbmp_gba_zones.md`](future_govt_bbmp_gba_zones.md) |
| Release notes per version | [`docs/play_store_release_notes_*.md`](play_store_release_notes_1.0.2.md) |
| Product defaults & thresholds | [`docs/product_assumptions.md`](product_assumptions.md) |

Planned or deferred work lives here unless marked **Shipped**. For release steps, see [release_and_versioning_guide.md](release_and_versioning_guide.md).

### After each implementation — update checklist

When you finish an enhancement (or a sub-phase such as **#10.1**), update **this file** in the same PR / commit:

1. **Item section** — set `**Status:** **Shipped** (vX.Y.Z)` or mark sub-phase done; add key files changed.
2. **Release roadmap** (bottom) — move item from “Next” to “Shipped” row if applicable.
3. **Priority reference** table — adjust what is “Next”.
4. **Linked docs** (only if scope changed):
   - GBA work → [`future_govt_bbmp_gba_zones.md`](future_govt_bbmp_gba_zones.md) “Current state” table
   - Store-facing copy → new or existing `play_store_release_notes_*.md`
   - Behaviour thresholds → [`product_assumptions.md`](product_assumptions.md)

Do **not** rely on chat or memory alone — **this backlog is the source of truth** for what is done vs pending.

---

## ★ Highest priority (do next)

> **#36 remaining (do first):** **36.1** hub types · **36.4** unregistered issue filter · **36.5** map look. **36.3 speed-breaker strip shipped.** **36.2 View-all charts shipped.** **36.6–36.9 shipped.**  
> **#33M reminder (periodic):** Retrain YOLO on Indian-road photos + Supabase stills; then heuristics→YOLO.  
> **ML Phase H — Hardening** (cloud assist, companion relay, rate limits, privacy) when ready.  
> **Phases A–G shipped** (YOLO, roles, IMU, 20s vision, Unconfirmed inbox, denser keyframes, swerve).  
> **#34 (Settings WIP gates):** Bump detection remains grayed (`IMU_BUMP_ENABLED`). Companion phone and USB camera are **on**.

---

## ML auto-detect pipeline (IMU + vision → report)

Phased product path for passive detection while navigating. Full flow:
Navigate → IMU bump / keyframe → still → YOLO → High auto-report / Medium Unconfirmed / Low drop.
See also [`ml_models_and_evaluation.md`](ml_models_and_evaluation.md).

### 33. On-device YOLO + auto-report pipeline

**Status:** **Phases A–G shipped** (Aug 2026). **Next: Phase H (optional hardening).**

| Phase | Scope | Status |
|-------|--------|--------|
| **A** | Train/wire YOLO on stills; `PotholeDetector` in validator + risk; bootstrap `.tflite` | **Shipped** |
| **B** | Device roles in Settings — *Nav + report* · *Camera only* · *Both* | **Shipped** |
| **C** | IMU “felt a bump” events | **Shipped** (Settings **grayed / WIP** — see **#34.1**) |
| **D** | Event → road keyframe “time travel” → YOLO → High/Med/Low | **Shipped** |
| **E** | Unconfirmed inbox UX (Confirm / Reject / Edit → `addReport`) | **Shipped** (avatar menu → Unconfirmed) |
| **F** | Richer short analysis window | **Shipped** (~2 fps / 20 s + post-event burst; no MediaRecorder) |
| **G** | Avoid/swerve lateral detection → Unconfirmed only | **Shipped** (`TripSwerveMonitor` / leave-rejoin) |
| **G+** | Continuous road-video YOLO scan (camera ON → auto-capture) | **Shipped** (`analyzeRoadVideo` + Settings toggle) |
| **H** | Hardening — cloud assist, companion relay, rate limits, privacy | **Next / planned** |

**Phase A key files:** `PotholeDetector.kt`, `PotholeDetectorGate.kt`, `PotholePhotoValidator.kt`, `PotholeRiskAnalyzer.kt`, `assets/ml/pothole_detect.tflite`.

**Phase B key files:** `TripDeviceRole` in `TripCompanionSettings.kt`, `CitizenUiSettingsRepository.applyTripDeviceRole`, `CitizenSettingsDialog.kt`.

**Phase C key files:** `TripImuBump.kt`, `TripImuBumpDetector.kt`, `TripBumpConfirmCard.kt`, HomeScreen Navigate wiring, Settings bump On/Off.  
**Note:** Bump On/Off is temporarily **disabled in Settings** (`TripFeatureGates.IMU_BUMP_ENABLED = false`) while other testing runs — tracked as enhancement **#34.1**.

**Phase D key files:** `TripRoadKeyframeBuffer.kt`, `TripBumpVisionGate.kt`, `TripBumpVisionPipeline.kt`, `TripUnconfirmedDetectionStore.kt`; camera still ticker in `TripRoadCameraPreview` / USB / companion publish.

**Design rules (locked):** signed-in only for auto-report; same duplicate + metro bounds; rate limit; user can disable auto-report; stills before continuous video; IMU on nav phone, eyes on road/companion cam.

**Enhancement (deferred):** after **5 confirms** near the same bump/pothole spot, publish a **public** map mark and sync across phones via Supabase. Personal marks on-device ship now; cross-device public crowd is not blocking.

### 33M. Major enhancement — Indian YOLO + continuous training loop (periodic)

**Status:** **Active process** (Aug 2026) — first India+public mix shipped in `assets/ml/pothole_detect.tflite`; hard reject still **off**. Recurring model work; do not block Phase H / product UX.

**Ask:** Keep improving detection as citizens upload real Indian evidence. Process + per-category counts: [`tools/train_pothole_model/MODEL_UPDATE_PROCESS.md`](../tools/train_pothole_model/MODEL_UPDATE_PROCESS.md).

| Track | Work |
|-------|------|
| **Retrain** | Collect Indian-road close-ups (+ ~30% hard negatives) → box in Roboflow → retrain YOLOv8 / YOLOv8-Seg → replace `assets/ml/pothole_detect.tflite` |
| **Sample targets** | Next useful: **150–200** boxed pothole close-ups, **80–120** wide, **80–120 manholes** (empty label), **150** other negatives. Solid / hard-reject review: **400–600** potholes + **200+** manholes. Bump/ditch as classes only after **300–500** each |
| **Heuristics → YOLO** | After precision/recall review on a frozen India set (mAP50 ≥ 0.70, manhole FP down), enable `YOLO_HARD_REJECT_ENABLED` and make YOLO the primary accept/reject gate (heuristics as fallback / assist) |
| **Supabase training loop** | Export `evidence` + `reports.photo_path` / `wide_photo_path`. Sort → box → mix with public set. Prefer Unconfirmed Confirm/Reject and officer “not a pothole” over raw submits. No nightly train on unlabeled uploads |
| **Cadence** | After **+50–100 new verified boxed** photos, or monthly once volume is steady; also each release that expands capture |
| **Ship** | TFLite stays in the APK until an OTA path exists. Update calibration notes + val metrics on every promote |

**Do now (no extra app code):** dashboard download from Storage `evidence` → folder sort → Roboflow boxes → `train_yolov8.py` → copy TFLite → rebuild.

**Automate later:** export script, review staging folder, dedup, CI/Colab train with promotion gates, optional OTA model (needs app work).

**Depends on:** Growing signed-in uploads; labeling time; frozen India test set.

**Does not replace:** Phase H / product UX. Heuristics stay the report gate until the manhole gate passes.

---

## Navigation map enhancements (after current trip steps)

### 31. Live traffic + journey-start ETA (MapLibre / navigation)

**Status:** **Phase A (journey-start ETA) shipped in code** — device-verify (Aug 2026). **Live traffic overlay (Phase B) still deferred.**

**Ask:** Let the user know what traffic will be like when they start the journey (leave-now delay, optional leave-later prediction), and optionally show congestion colors on the trip map.

**Today:** MapLibre + OpenFreeMap / MapTiler Streets. OSRM ETAs are **static** (no congestion). Do not claim “less traffic” / “live traffic” in UI until a real feed is wired.

**Shipped (Phase A):** `JourneyStartEta` — India metro hour-of-week leave-now factor vs free-flow OSRM. Planner shows **“Leave now ~X · Usually Y”** (amber when congested). Alternate chips use **Now ~X** when congested. No map congestion colors.

**Still deferred:** Phase B map colors, Phase C leave-later, Phase D crowd, Mappls/TomTom client when keys are available.

**Research notes (1 Aug 2026):**

| Path | Role | Cost reality |
|------|------|----------------|
| **Mappls (MapMyIndia)** | Best India fit for **leave-now** traffic ETA + **predictive** “leave at 8 AM” | Free dev signup; production usually paid |
| **TomTom Traffic** | India flow/incidents; MapLibre green/yellow/red overlay | Freemium → paid at scale |
| **MapTiler** | Basemap already keyed; not a free city-wide traffic feed | Free tier = maps limits, not Google-like traffic |
| **Crowd GPS (own users)** | Long-term free signal while Navigation is on | Needs density + consent; weak until many concurrent trips |
| Public OSRM / OpenFreeMap | Basemap + routing only | **No** live traffic |

**Phased scope (when un-parked):**

1. **Phase A — Leave now:** On Direction / Navigation, show traffic-aware ETA vs static OSRM (e.g. Mappls `route_eta` / `route_traffic`, or TomTom flow along the route). Copy like “Now 28 min · Usually 18 min”.
2. **Phase B — Map colors:** Congestion overlay on fullscreen trip map (TomTom/Mappls tiles); toggle Traffic on/off.
3. **Phase C — Leave later:** Predictive ETA for a chosen departure time (Mappls predictive, or own hour-of-week averages).
4. **Phase D — Crowd fill:** Opt-in anonymized speed samples during Navigation; color only high-confidence segments; never invent congestion.

**Product rules:** Prefer India coverage; keep OSRM geometry until traffic-aware routing is an explicit switch; privacy/consent for any crowd upload.

**Key files (when built):** `CitizenMapTiler.kt`, `MapLibreDensityMap.kt`, `OsrmRouteClient` / new traffic client, trip planner ETA UI, `local.properties` secrets, `docs/product_assumptions.md`.

---

### 32. Trip capture source (camera / camera+map / USB / companion)

**Status:** **Phase 0–6 shipped in code** (Aug 2026).

**Ask:** During navigation, let the user choose how road (and optional cabin) cameras feed capture and analysis — phone lenses, USB cams, or another phone on the same account — and how the trip UI shows map vs live camera.

**Model:**

| Concept | Options |
|---------|---------|
| **Display** | Map only · Camera · Camera + map (Switch → Split) |
| **Road (rear)** | Phone back · Phone front · USB 1 · USB 2 · Companion |
| **Cabin (front)** | Off · Phone front/back · USB 1/2 · Companion |
| **Applies when** | Navigate active |
| **Capture kind** | Stills for reports (no continuous video in early phases) |

**Phases:**

| Phase | Scope | Status |
|-------|--------|--------|
| **0** | Settings UI + prefs | **Shipped** |
| **1** | Full-screen road camera (phone back/front) during Navigate; Report on camera chrome | **Shipped** |
| **2a** | Camera ↔ map Switch (left-rail toggle) | **Shipped** |
| **2b** | Split — landscape side-by-side · portrait stacked (camera | map) | **Shipped** |
| **3** | Phone front/back role assignment; cabin PIP when other lens + concurrent support | **Shipped** |
| **4** | Companion phone — pairing code + Wi‑Fi / hotspot / **USB tethering** stills | **Shipped** — Settings enabled (**#34.2**) |
| **5** | USB UVC experimental — inventory, permission, CameraX external when OEM exposes it | **Shipped in code** — **Settings grayed / WIP** (**#34.3**) |
| **6** | Multi-feed analysis on road stills → Report close-up | **Shipped** |

**Phase 4 transport decision:** Same Wi‑Fi is **not** a forever product requirement (cloud/account relay can come later). Phase 4 path: local HTTP stills + pairing code. Navigator discovers via NSD on Wi‑Fi **and** probes USB-tether IPs (RNDIS/NCM, typically 192.168.42.x) when a cable is used. A second Android phone is **not** a UVC webcam — do not map it to USB camera 1.

**Phase 5 USB decision:** Host-mode UVC (`com.herohan:UVCAndroid`) when the OEM does not expose the webcam through CameraX. Detect UVC-class devices (not MTP/RNDIS phones), request permission, preview via CameraX external or UVC. Dual-USB preview of two UVC streams at once is deferred.

**Phase 6 decision:** Road stills only (cabin excluded). Live CameraX road session snaps on Report when bound; companion / USB stills publish into `TripRoadStillStore` (~90 s freshness). When primary + secondary exist, `PotholePhotoValidator` pick-best → FileProvider URI prefills `NewReportScreen` and re-runs close-up validation + risk analyzer.

**Phase 0–6 files:** prior Phase 0–5 files + `TripRoadStillStore.kt`, `TripRoadStillCapturer.kt`, `TripRoadCloseUpPrefill.kt`, ImageCapture on phone/USB road preview, `NewReportScreen(initialCloseUpUri)`.

**Product notes:** Normal Report wizard unchanged. Lens ≠ USB — USB is a source assigned to Road/Cabin roles. Companion is a separate device path, not a phone lens. CameraX preview; Report opens the existing signed-in / sign-in report path. Cabin PIP needs device concurrent-camera support; otherwise Road stays live with a short note.

**Settings WIP (Aug 2026):** Bump detection stays grayed (`IMU_BUMP_ENABLED`). Companion + USB camera chips are enabled.

---

### 34. Re-enable WIP-gated trip features (bump / companion / USB)

**Status:** **Enhancement — bump still gated** (Aug 2026). Companion + USB camera are enabled in Settings.

**Ask:** Finish reliability work, then turn the Settings controls back on so citizens can use bump sensing, companion phone road feed, and USB cameras again.

| Sub | Feature | Settings today | Gate flag | Done when |
|-----|---------|----------------|-----------|-----------|
| **34.1** | **Bump detection** (IMU → Unconfirmed / auto-draft) | Grayed · forced Off · “Work in progress” | `TripFeatureGates.IMU_BUMP_ENABLED` | Field-stable bump UX; set flag `true` |
| **34.2** | **Companion phone** (Nav + report / Camera only · pairing · Road = Another phone · Wi‑Fi or USB tethering stills) | **Enabled** | `TripFeatureGates.COMPANION_CAMERA_ENABLED` | Reliable link + stills; set flag `true` |
| **34.3** | **USB camera 1/2** (road / cabin OTG · CameraX external) | Road + cabin chips grayed · “Work in progress” | `TripFeatureGates.USB_CAMERA_ENABLED` | Stable preview/capture on target devices; set flag `true` |

**Key files:** `TripFeatureGates.kt`, `CitizenSettingsDialog.kt`, `CitizenUiSettingsRepository.kt`, `TripCaptureSettings.kt` (selectable), companion/USB preview + LAN (`TripCompanionLan`, `TripUsbCameraPreview`).

**Related:** **#32** Phase 4–5, **#33** Phase C, optional **#33** Phase H companion cloud relay later.

---

## Parked highest priority (user reminder 27 Jul 2026)

> **Focus lock:** Near-term = trip/navigation polish. **#31** stays in **Navigation map enhancements** until un-parked after those steps.

### 23. Trip alternate-route quality + deviation handling (MOST IMPORTANT — revisit)

**Status:** **Shipped in code** — device-verify remaining. Tap-to-confirm for known alts; hybrid rejoin uses ~400 m / 90 s thresholds; MapLibre draws rejoin + offered alternate.

**Shipped in code (device-verify):**

| Piece | Detail |
|-------|--------|
| **23.1** | Soft max routes **3** (1 primary + ≤2 alts) |
| **23.2** | When ETAs within **~120 s**, reorder by severity-weighted pothole risk; chip **“Similar time · fewer severe potholes”** |
| **23.3 offer** | Preview corridors kept after Navigate; if off-route near a known alt → orange **Tap to use** (no auto-switch) |
| **23.3 rejoin** | Hybrid: dashed **teal** return line when return ≤ ~400 m (and ETA within ~90 s when probed); else full OSRM reroute |
| **23.3 clear** | Rejoin / offer chrome clears when back on corridor |
| **Covered route** | Traveled segment is **hidden** on the map (remaining blue only) |

**Still later:** Device tune of the 120 s ETA window / weights if field tests need it.

**Problem today (pre-change):** Alternates sometimes **missing**, or include **waste / meaningless diversions**. Quality filters and via-probes need a better Google-like balance. Reroute on deviation existed (~60 m off-route) but did not distinguish “user chose a better corridor” vs “user should rejoin.”

**Product goal:** Show **fewer, better** routes — not more routes for the sake of it.

| Phase | Scope |
|-------|--------|
| **23.1 — Alternate quality (ship first)** | **1 primary + up to 2 genuinely useful alternates.** Reject near-duplicates, spurs, messy U-turn loops, and fake detours. Prefer real corridor differences (parallel road, ORR vs inner, etc.). |
| **23.2 — Pothole-aware ranking (later)** | When ETAs are close, rank routes by **road condition / pothole risk** (fewer severe/critical ahead, not just fastest). Makes City Grid more useful than plain navigation. |
| **23.3 — Deviation / detour UX** | If the user leaves the active route: **(A)** if they are on or near a **known good alternate** (or a clearly better next path), switch guidance to that route; **(B)** otherwise show how to **rejoin the original route** (rejoin segment / “return to route” cue) rather than always blind full reroute. |

**23.1 — Quality rules (draft):**

- Do **not** optimize for route count — optimize for **trust**.
- Native OSRM alternatives first; via-probes only when needed (see `product_assumptions.md` §5.3).
- Reject: too similar to primary, out-and-back spurs, excess U-turns, gross corridor waste, bike-only scraps beside a road primary.
- Soft fallback: if strict filter leaves &lt; 2 options, keep **light-filtered distinct** corridors rather than showing nothing.

**23.2 — Ranking (shipped in code):**

- Default sort: **ETA** (static OSRM — no live traffic yet).
- When time delta ≤ **~120 s**, prefer route with **lower pothole risk** (severity-weighted: Minor 1 · Moderate 2 · Severe 4 · Critical 6).
- Surface in UI: **“Similar time · fewer severe potholes”** on the safer chip.
- Key files: `TripRoutePotholeRanking.kt`, `HomeScreen.kt` (`rankTripRoutesByPotholeRisk`).

**23.3 — When user deviates (draft):**

| Situation | Behaviour |
|-----------|-----------|
| Slight drift / GPS jitter | No drama — existing snap + tolerance |
| Off original route, **on a known alternate** that still reaches the destination | **Auto-continue** that alternate for the chosen vehicle (no tap). |
| On a corridor that **does not reach the destination** | Dashed **rejoin** onto a route that does |
| On a path **unsuitable for the vehicle** (e.g. car on jungle / dirt-bike trail — poor OSRM snap) | **U-turn** + rematch on the vehicle network |
| Off route, otherwise | **Full reroute** for the best vehicle-appropriate path to the destination |

**Decided (updated Aug 2026):**

| Question | Decision |
|----------|----------|
| Auto-switch to alternate vs tap | **Auto-adopt** when GPS is on a known preview alternate that still reaches the destination |
| Rejoin line on map | **Only** when the corridor the driver is on does **not** meet the destination (or tiny GPS spur). Never while continuing a valid dest-reaching alternate |
| Vehicle-aware recovery | Use trip vehicle (Car/Bike/Walk) OSRM profile; wrong network → U-turn |
| Pothole ranking weights (#23.2) | **Shipped** — Minor 1 · Moderate 2 · Severe 4 · Critical 6; ETA window **~120 s** |
| Rejoin vs reroute | Rejoin for dead-end / missing-dest corridors; else rematch best vehicle route |

**Hybrid threshold (draft — tune on device):**

| Rule | Draft value |
|------|-------------|
| Prefer **rejoin** when | Return-to-route distance ≤ **~400 m** **and** rejoin ETA is not much worse than full reroute (draft: within **~90 s**) |
| Prefer **full reroute** when | Return &gt; **~400–500 m**, or rejoin ETA clearly worse, or user is heading away / past the corridor |
| Still show dashed rejoin line | While hybrid says “rejoin is viable”; hide / replace with new route once full reroute is chosen |

Numbers above are starting points — adjust after road tests in Bengaluru traffic.

**When resuming:** Re-read `docs/product_assumptions.md` §5 and `OsrmRouteClient.kt`; implement **23.1** before **23.2** or **23.3** polish.

**Key files:** `OsrmRouteClient.kt`, `OsrmRouteAlternatesTest.kt`, `TripNavigationMatcher.kt` (off-route), `TripNavigationOverlay.kt`, `HomeScreen.kt` (reroute / preview), `docs/product_assumptions.md` §5.

---

### 24. Separate home map vs trip fullscreen map (UX — agreed, not coded yet)

**Status:** **Implemented** (25 Jul 2026) — verify on device; tweak if needed.

**Rules (confirmed with user):**

| Surface | Behaviour |
|---------|-----------|
| **Home page map** | **Reports only.** Never draw trip/navigation polylines, ETA bubbles, or nav chrome — even if a trip is still active and the user **minimized** fullscreen. |
| **Fullscreen trip / navigation map** | Owns all navigation UI. User may **pan the whole map** (not locked to city boundary). |
| **Minimize while Navigate is on** | Fullscreen collapses; **Trip chip stays red** only if Navigate already started (`tripNavActive`). Home map = reports only. |
| **Minimize while only planning** (no dest / dest cleared / Navigate not pressed) | Fullscreen collapses and **Trip turns off** — chip is **not** red on home. |
| **Tap Trip again while minimized + Navigate active** | **Resume** the same trip fullscreen — do **not** end or restart. |
| **Turning Trip off** | Explicit off while in fullscreen Navigate ends the session; or start a new plan when chip is not red. |

**Intent:** Home = city reports / heat / clusters. Trip = dedicated navigation surface with free pan. Minimize hides the trip map but keeps the trip alive via the Trip chip.

**Key files:** `HomeScreen.kt` (fullscreen vs embedded map, trip overlay gate, metro pan bounds).

---

### 25. Clearer map labels when zoomed in

**Status:** **25.1 shipped in code (27 Jul 2026)** — device-verify. **25.2** / **25.3** deferred (deeper readability + bilingual regional names).

**Ask:** Increase place / street label clarity at higher zoom so names stay readable (especially in street-follow / trip zoom).

**Why 25.1 vs later:** Basemap labels are **raster** (CARTO Voyager tiles). We cannot truly enlarge glyph size or swap languages without a separate name layer — so this pass improves **contrast + install path**; true font-size / denser names are **#25.2**; bilingual regional + English is **#25.3**.

#### 25.1 — Readable contrast now (done)

- [x] Install `installCartoLabelDarkeningTilesOverlay()` on home map + report-detail map (was defined but unused)
- [x] Stronger mid-grey → charcoal remap tuned for Voyager street/place ink
- [x] Keep pale roads / water / parks untouched; bilinear filter on overzoom
- [x] Unit tests for remap behaviour

**Key files:** `CartoBasemapLabelDarkener.kt`, `HomeScreen.kt`, `ReportDetailDialog.kt`, `CartoBasemapLabelDarkenerTest.kt`.

#### 25.2 — In-depth label readability (later — important)

**Status:** **Shipped in code** — device-verify (Aug 2026). Overpass street/place name overlay at zoom ≥15 (MapLibre SymbolLayer + OSM markers); stronger CARTO label darken; hidden when Heat is on / city overview.

#### 25.3 — Bilingual labels: English + regional / chosen language (later)

**Status:** Not implemented — **enhancement** (captured 27 Jul 2026). No code yet.

**Ask:** Along with the **English** basemap label, show the **regional / native** name for that place (e.g. English + ಕನ್ನಡ / Hindi), so users can recognize places both ways.

| Rule | Detail |
|------|--------|
| **Default** | Regional language from **GPS region / metro native** (e.g. Bengaluru → Kannada alongside English) |
| **User control** | Setting to pick which regional language to show with English (or turn regional off) |
| **Display** | Prefer **both** on map when space allows (English + selected language), not replace English by default |
| **Translate / understand** | User can change language as needed so labels match what they understand |

**Depends on:** A name overlay / vector source with multilingual OSM tags (`name`, `name:en`, `name:kn`, …) — raster Voyager tiles alone cannot swap languages. Builds naturally after **#25.2**.

**Open questions (edit later):**

- [ ] Dual-line vs “Name (regional)” on one line?
- [ ] Languages offered (Kannada / Hindi / Tamil / Telugu / …) vs free locale pick?
- [ ] Trip search results also bilingual?
- [ ] Offline: cache regional names for visible tiles?

---

### 26. Adopt more Google Maps–like traits (product think)

**Status:** **Shipped in code** (Aug 2026) — Recenter FAB (blue nav icon) on osmdroid + MapLibre; next-maneuver first in capsule; alternate chips single-line + MapLibre “Tap to use alternate” chip; journey-start ETA on planner. Skip 3D tilt on osmdroid (documented limit).

**Ask:** Study and selectively copy useful Google Maps behaviours for trip + city map (without claiming to be GMaps or requiring their SDK).

**Shipped trait set:**

- [x] Recenter / follow re-arm (blue FAB when panned away)
- [x] Next-maneuver priority in top capsule
- [x] Alternate chips + mid-nav tap-to-use
- [x] Responsive chrome + speed-linked street zoom (#22 polish)
- [ ] Camera feel further tune (follow lag) — device-verify
- [ ] Search result titles vs address hierarchy (partially done)
- [x] Gesture polish (two-finger tilt N/A on osmdroid 2D — documented)

---

### 27. Soft edge “danger blink” for severe / critical ahead (Witcher-style)

**Status:** **Shipped in code (27 Jul 2026)** — device-verify. Product locked rules below.

**Ask:** During navigation, when a **Severe** or **Critical** pothole (or cluster) is ahead, show a soft game-like blink / pulse on roughly **three screen edges** (left, right, and top — not a full red overlay), inspired by damage/danger vignettes (Witcher-style), not a harsh full-screen flash.

**Intent:** Peripheral danger cue without blocking the road view or the speed/turn capsule.

**Implemented:**

| Piece | Detail |
|-------|--------|
| Logic | `TripDangerBlink` — zones, speed start distances, L/R/S, Critical vs Severe |
| UI | `TripDangerEdgeBlink` under trip chrome (fullscreen Navigate) |
| Mute | Blink **still shows** when voice muted |

#### Locked product rules (27 Jul 2026)

| Topic | Declaration |
|-------|-------------|
| Who gets blink | **Severe** and **Critical** only (Minor / Moderate: no edge blink for now) |
| Mute | **Blink stays on** when voice is muted (eyes-only warning) |
| Side emphasis | Pulse **stronger toward hazard side**: **L** / **R** / **S** (straight / middle / full-road) |
| Stop when | Keep pulsing until the hole / **cluster end** is **passed** (do not cut off at ~15 m) |
| Critical vs Severe | **Critical** pulse is **harder and faster** than Severe (worst severity in zone) |
| Trigger distance | **Speed-scaled** — slow **150 m** / mid **200 m** / fast **250 m** |
| Close holes | **Cluster** if consecutive Severe/Critical within **≤ 7 m** along route |

**Key files:** `TripDangerBlink.kt`, `TripDangerEdgeBlink.kt`, `HomeScreen.kt`, `TripDangerBlinkTest.kt`, [`product_assumptions.md`](product_assumptions.md) §5.6.

---

### 28. Wide-shot on-screen L / M / R side selection (combine steps)

**Status:** Implemented (27 Jul 2026) — enhancement.

**Ask:** While confirming the **wide shot**, tap **Left / Middle / Right** on the photo (contiguous multi-select). One step covers capture + position + traffic Flip.

**Shipped behaviour:**

| Rule | Detail |
|------|--------|
| Spans | Contiguous only: **L, M, R, LM, MR, LMR** (order irrelevant). **LR blocked** |
| LMR | Replaces separate **Full road** control (storage code `LMR`; legacy `F` still maps) |
| Section 04 | **Removed** — wide-shot span is source of truth (Adjust lanes on summary) |
| Guidelines | Dashed road-border overlay; soft ~**85%** alignment warn (not hard block) |
| Traffic | Animated arrows toward camera; **Flip** → against traffic (`AWAY`) |
| Arrow opacity | Scales with **screen brightness** (stronger in sun) |

**Key files:** `WideShotLaneConfirmDialog.kt`, `PotholeLaneSpan.kt`, `PotholePosition.kt`, `NewReportScreen.kt`, `ScreenBrightnessOpacity.kt`, `WideShotAlignmentGate.kt`.

**Field test:** Deferred — user will exercise #28 after other open work. Fixed trapezoid guidelines for now.

**Follow-up (important, later):** **#30** — auto-scan road borders so digital guidelines snap to the real edges.

---

### 29. Turn-aware, speed-scaled pothole alerts

**Status:** **Shipped in code** — device-verify (Aug 2026). Shared [TripAlertScheduler] orders turn vs pothole by route meters, coalesces &lt;~40 m, severity×speed horizons / repeats; mute skips TTS (haptic on first still).

**Ask:** When both a **turn** and a **pothole** are approaching, schedule alerts from **order on the route** and **driving speed**.

| Situation | Behaviour |
|-----------|-----------|
| **Pothole before the turn** | Warn about the pothole **earlier** (before the turn cue dominates). Low speed → **1–2** alerts. Higher speed → **multiple** alerts before reaching it. |
| **Pothole after the turn** | **Once before the turn** (preview what’s next after turning), then after the turn **1 / 2 / multiple** times depending on speed. |

**Accepted product extras (user OK 27 Jul 2026):**

1. **Coalesce when close** — If turn and pothole are very close along the route (draft: &lt; ~40 m), use **one short combined cue** (e.g. “Pothole, then left”) instead of two competing alerts.
2. **Severity weighting** — **Severe / Critical** get earlier / stronger / more repeats; **Minor** stays quieter. Pair voice/capsule with **#27** edge blink for severe/critical only.

**Recommended implementation approach (refine on build):**

Prefer **time-to-event** (or distance ÷ speed) over fixed “N beeps”:

- Alert budget ≈ how many reminders fit before ETA to the pothole (e.g. slow ≈ 1–2, fast ≈ 3–4), with a **minimum gap** between repeats so it doesn’t nag.
- **Turn vs pothole order** from route geometry (meters along remaining path), not screen order.
- Shared alert scheduler with **#22** / **#27** (one brain for voice + capsule + edge blink).
- Optional stretch: several potholes in one segment → one summary (“3 potholes ahead”) then side/severity for the next one.

**Key files:** `TripNavigationMatcher.kt`, `TripPotholeAlertFeedback.kt`, `HomeScreen.kt` (nav chrome / TTS), step list from OSRM.

---

### 30. Auto-match digital road guidelines to real road borders (scan → snap)

**Status:** **Shipped in code** — device-verify (Aug 2026). Classical luminance/gradient border scan (`RoadBorderSnap`); snaps dashed guidelines when confidence is strong; falls back to fixed trapezoid; soft gate unchanged.

**Ask:** On the wide-shot confirm screen, **scan** the photo for actual left/right road borders, then snap the dashed digital guidelines (and L/M/R thirds) to that detected road polygon — including when the reporter stands at the curb (skewed perspective).

**Why important:** Fixed trapezoid forces the user to frame to the template. Auto-snap makes L/M/R mean “thirds of the real road,” which is more accurate for trip alerts.

**Product shape (locked for backlog):**

| Piece | Intent |
|-------|--------|
| Detect | Road edges / carriageway mask from the wide photo |
| Snap | Guidelines follow detected borders; L/M/R partition that width |
| Soft gate | Keep ~**85%** confidence idea — warn if lock is weak; don’t hard-block |
| Fallback | If scan fails, keep today’s fixed trapezoid |
| Optional later | User fine-drag edges when auto is close but not perfect |

**Approach options (decide on build):** classical CV / light segmentation / hybrid auto + drag. Prefer hybrid for field robustness.

**Illustration:** `enhancement-28-auto-road-border-match.png` (assets / canvases discussion 28 Jul 2026).

**Depends on:** #28 shipped UX (confirm overlay, Flip, contiguous spans).

**Key files (likely):** `WideShotLaneConfirmDialog.kt`, `WideShotAlignmentGate.kt`, possibly new road-edge helper / ML path.

---

### Review notes — better possibilities (27 Jul 2026, no code yet)

Cross-check of open enhancements. Treat as **proposals** until promoted to numbered items.

| # | Idea | Why it helps |
|---|------|--------------|
| **A** | One **alert scheduler** for #22 + #27 + #29 (voice, capsule, edge blink) | Avoids three systems fighting for attention at a turn |
| **B** | On **#23** alternates: prefer corridors with **fewer severe/critical** ahead when time delta is small | Unique City Grid value vs plain GMaps ETA |
| **C** | **#25.1** contrast done; **#25.2** larger/vector names; **#25.3** English + regional language (GPS-default, user-selectable) | Raster can’t enlarge or translate glyphs — overlay after 25.1 verified |
| **D** | **#26** GMaps traits — ship only: strong **Recenter**, clear **next-maneuver priority**, better **alternate chips**; skip 3D tilt on osmdroid | Highest UX return, honest about 2D limits |
| **E** | **#28** wide-shot L/M/R: optional **auto-suggest** side from photo blob, user taps to confirm | Faster + still “as per the photo” |
| **H** | **#30** auto-snap guidelines to **scanned road borders** (after #28 field test) | L/M/R = thirds of real road, curb-friendly |
| **F** | Cluster of potholes before a turn → **one count cue**, then detail the next hole | Stops alert spam on bad stretches |
| **G** | Edge blink (#27) / repeats (#29) must **respect mute / volume** | Accessibility + night driving |

---

## Auth & citizen identity

### 1. Separate account per email (current design — no app work)

**Status:** Shipped by default (Supabase Auth behaviour).

Each successful signup with a **new, unused** email creates:

- a new row in `auth.users` (`auth_id`)
- a new `citizen_profiles` row and `PW-xxx` reporter id
- a separate login and separate “My Reports” scope

There is **no account linking**. A person who signs up with `a@gmail.com` and later with `b@gmail.com` (if both are unused) has **two independent accounts**.

**Duplicate email blocked:** signup calls the `email_exists` RPC (`0002_email_exists.sql`) before `signUpWith`. Existing Auth emails show: *“An account with this email already exists. Sign in instead.”*

---

### 2. Account linking

**Status:** Not implemented — future feature.

**Goal:** Allow one real person to tie **multiple email addresses** to a **single** citizen identity (`auth_id`, one `PW-xxx`, one merged “My Reports” history).

**Why deferred:** Current product uses one email = one account. Linking needs explicit UX (verify both emails, merge reports, handle conflicts) and server rules (which `auth_id` survives, what happens to the second account’s reports).

**Rough scope when picked up:**

- [ ] Design: primary account + add/verify secondary email, or merge two existing accounts
- [ ] Supabase: schema/RPC for linked identities or single auth user with multiple verified emails
- [ ] App: settings flow + merge `citizen_profiles` / reassign `reports.reporter_auth_id` where needed
- [ ] Prevent abuse: rate limits, both-inbox verification, audit log

---

### 3. Email change history (audit trail)

**Status:** Not implemented — optional future.

**Current behaviour:** Only the **current** email is stored in `auth.users` and mirrored to `citizen_profiles.email` (migration `0009_citizen_profile_email_sync.sql`). Old addresses are not kept in `citizen_profiles`.

**If needed later:** add e.g. `citizen_email_history (auth_id, email, changed_at)` for support/audit — not required for duplicate signup prevention (`email_exists` uses `auth.users` only).

---

## Reports & sync

### 4. Download report photos from Supabase on sync

**Status:** Shipped (report photo cache + sync in citizen app).

Signed-in and public feed sync download evidence photos into app storage via
`ReportPhotoCache`. See `ReportSyncRepository` / `RecentReportsRepository`.

---

### 5. Stricter city / metro match on report submit

**Status:** **Shipped** (v1.0.2).

Validate that report GPS / picked map location lies inside the selected city boundary before submit. Bengaluru uses official **GBA polygon** (`CityMetroLocation.validateSubmitLocation`, `BengaluruGbaBoundary`).

**Files:** `CityMetroLocation.kt`, `NewReportScreen.kt`, `BengaluruGbaBoundary.kt`.

---

## UI & notifications

### 6. In-app notifications bell

**Status:** **Shipped** (v1.0.2).

Header bell for citizen notifications (app update prompts, report status messages) wired to `CitizenNotificationsRepository`.

**Files:** `CitizenNotificationsRepository.kt`, `NotificationsDialog.kt`, `HomeScreen.kt`, `RecentReportsRepository.kt`.

---

### 7. Email verification settings tab (admin / template polish)

**Status:** Partially deferred.

Signup and profile email change rely on Supabase email templates using `{{ .Token }}` (Confirm signup, Change email address) and **Secure email change off** for a single-code profile update. Operator checklist: [`docs/supabase_email_templates.md`](supabase_email_templates.md).

---

### 8. Report submission confirmation email

**Status:** **Shipped** — deployed and verified (July 2026).

After a **signed-in** citizen’s report is inserted into `public.reports`, a Database Webhook calls `notify-citizen-report-created` to email the reporter at their **registered Supabase Auth email** via Brevo.

**By design — not for guests:** Reports with no `reporter_auth_id` skip the email (anonymous / guest submit). No guest confirmation email is planned.

| Item | Decision |
|------|--------|
| Who gets mail | Signed-in users only (`reporter_auth_id` set) |
| Ticket in email | `CG-` + last 8 digits of `reports.id` |
| User ID in email | Raw `reporter_user_id` (`PW-xxx`) |
| Timestamp | IST |
| Idempotency | `report_email_log` |

**Files / ops:** `supabase/migrations/0010_report_email_log.sql`, `supabase/functions/notify-citizen-report-created/index.ts`. Deploy guide: [`docs/report_confirmation_email.md`](report_confirmation_email.md).

---

## Government / cross-app

### 9. Deeper two-way sync with CG GOVT app

**Status:** **Parked** — government app backlog (citizen push already works).

Citizen push to `reports` + `evidence` is implemented. Full round-trip (status transitions, proof photos in My Reports) is **CG GOVT work** — see [government_app_handover.md](government_app_handover.md) § **G4**.

---

### 10. Bengaluru GBA / municipal alignment

**Status:** **Citizen shipped** (v1.0.3) — boundary, 5 corp routing, ward at submit, Accountability copy. **Gov/seed work parked** — see [government_app_handover.md](government_app_handover.md) § G2–G3.

**Problem:** Map and report eligibility should match **2025 GBA** structure. Boundary is aligned; **officer routing** still uses legacy **8 BBMP zones**.

**Full draft (options A–D, ward-level, files):**  
[`docs/future_govt_bbmp_gba_zones.md`](future_govt_bbmp_gba_zones.md)

#### 10.0 — GBA official outer boundary (map + submit)

**Status:** **Shipped** (v1.0.2).

| What | Detail |
|------|--------|
| Red city outline | Official Sept 2025 GBA polygon (OpenCity KML, simplified) |
| Pan / zoom limits | GBA bounding box (replaces oversized hand-tuned bbox) |
| Report eligibility | Point-in-polygon for Bengaluru submit + map pick |
| Asset | `assets/bengaluru_gba_boundary.json` |
| Regenerate | `python tools/generate_bengaluru_gba_boundary_assets.py` |

**Files:** `BengaluruGbaBoundary.kt`, `HomeScreen.kt`, `CityMetroLocation.kt`, `IndiaCityMapCatalog.kt`, `MainActivity.kt`.

#### 10.1–10.4 — GBA corporation routing migration (Option B)

**Status:** Not started — **recommended next GBA work** after 1.0.2 ships.

| Phase | Work | Scope | Status |
|-------|------|-------|--------|
| **1** | Add **5 GBA corporation assignee keys + centroids** | `MunicipalContactsRegistry.kt` — keys `BENGALURU:GBA_CENTRAL` … `GBA_WEST`; legacy 8 BBMP keys kept for lookup | **Shipped** (v1.0.3 dev) |
| **2** | Update **`officers.json` / Supabase seed** with GBA commissioners | **Done in repo** (16 Jul 2026) — names in seed + `MunicipalContactsRegistry`; re-run `seed_officers.mjs` on Supabase when deploying gov | **Shipped (data)** — see [`gba_official_contacts.md`](gba_official_contacts.md) |
| **3** | **Re-map existing reports** (optional backfill) or **new reports only** | **Forward-only** — historical rows keep old 8-zone `assignee_key` | **Decided** |
| **4** | **Accountability UI labels** — “East Corporation” vs “East Zone” | `AccountabilitySection.kt` GBA copy | **Shipped** (v1.0.3 dev) |

**Suggested implementation order:** Phase **1 → 2 → 4** in one release; Phase **3** decided before seed deploy (backfill vs forward-only).

**Depends on:** Verified commissioner names — snapshot in [`gba_official_contacts.md`](gba_official_contacts.md) (16 Jul 2026). Ward officers still pending public GBA roster.

#### 10.5 — Ward-level routing at submit (citizen app)

**Status:** **Shipped** (v1.0.3 dev) — citizen submit + report details only; **not** on home map.

| Scope | Detail |
|-------|--------|
| **Citizen app** | GPS → 369 official GBA ward polygons → corporation + ward snapshot on new reports; shown in submit snackbar, report details, Accountability tab |
| **Home map** | GBA outer boundary only — **no** corp/ward overlays (by design) |
| **Gov app** | Ward/corp map overlays + ward officers — separate backlog; see [`future_govt_bbmp_gba_zones.md`](future_govt_bbmp_gba_zones.md) Option C |
| **DB** | `supabase/migrations/0011_report_ward_routing.sql` — apply before cloud sync includes ward columns |
| **Assets** | `bengaluru_gba_wards.json` (369 wards); regenerate via `tools/generate_bengaluru_gba_wards_assets.py` |
| **Historical reports** | Forward-only — no backfill of ward fields on old BBMP-zone rows |

**Not in scope (later):** 369 ward **officers**, gov-app ward map, citizen home map ward boundaries.

#### 10.6 — Zone / corporation reference UI (Option A, optional)

Read-only citizen screen listing municipal units + officer office addresses; can ship alongside Phase 4.

**Reminder trigger:** Before BBMP/GBA official pilot, or when stakeholders ask for Central/North corporation names in Accountability.

---

### 11. Signup interrupted after “Send verification code” (stuck / “email already exists”)

**Status:** **Shipped** (v1.0.2).

**Primary surface:** **Full signup screen** — `SignupScreen.kt` via Login → Create account (`AuthNavHost`).  
**Also apply later for consistency:** `ReportSignInModal.kt` (home report flow).

**Problem (confirmed on full signup screen):**

1. User fills signup (full name, email, password) and taps **Send verification code**.
2. Supabase creates an **unconfirmed** `auth.users` row and emails the OTP.
3. User leaves the screen before entering the code — **Back to login**, **Log in** link, or **system back** (not an outside-tap dismiss on this screen).
4. Navigation pops `SignupScreen` → UI state is lost; verification step disappears.
5. User opens signup again and fills the **same email** → error: **“An account with this email already exists. Sign in instead.”**
6. User is stuck: not verified, cannot complete signup; message implies sign-in (which may fail until verified).

**Where it happens:**

| Surface | File | How user leaves mid-OTP |
|---------|------|-------------------------|
| **Full signup screen (primary)** | `SignupScreen.kt` via `AuthNavHost` | Back to login, Log in, system back — `rememberSaveable` does not survive route pop |
| Report sign-in modal (secondary) | `ReportSignInModal.kt` | Outside tap / close on `Dialog` |

**Root cause (technical):**

- `SupabaseAuthRepository.startEmailSignup` calls `signUpWith(Email)` → account exists in Supabase immediately, even if OTP not verified.
- Before signup, `email_exists` RPC (`0002_email_exists.sql`) returns true for **any** row in `auth.users`, including **unconfirmed** signups.
- Second `startEmailSignup` → `EMAIL_ALREADY_REGISTERED` before Supabase can resend.
- OTP UI state (`verificationRequested`, code field) lives inside `SignupScreen`; leaving the route disposes the composable.

**Proposed fixes (pick combination when implementing):**

| # | Fix | Layer | Notes |
|---|-----|--------|--------|
| A | **Confirm before leaving** during OTP: warn on Back to login / system back — “Verification pending. Leave anyway?” | UI | **Keep / leave** dialog — not Option D |
| B | **Hoist pending signup state** to `AuthNavHost` (or small holder): email + `verificationRequested` survives re-entering signup | UI | For modal later: hoist in `HomeScreen` |
| C | On reopen with pending email, **restore OTP step** + **Resend code** | UI | |
| D | **Clearer copy only** — e.g. “Verification pending… enter code or resend” instead of “Sign in instead” | UX text | Does **not** add keep/undo by itself |
| E | **Resend OTP** for unconfirmed users instead of hard block when `email_exists` | Auth | |
| F | Optional RPC: confirmed vs unconfirmed email (e.g. `email_confirmed_at`) | Supabase | |
| G | Mirror A–E on `ReportSignInModal` for consistency | UI | After full signup screen is fixed |

**Suggested approach (full signup first):** **A + B + C + E**, add **D** copy if needed, then **G** for modal.

**Acceptance criteria:**

- [ ] Leaving signup during OTP warns first (Option A) or state is preserved (Option B).
- [ ] Reopening signup with same pending email shows code entry + resend, not “already exists” dead end.
- [ ] Completing OTP still creates session and citizen profile as today.
- [ ] No duplicate `citizen_profiles` / `PW-xxx` for one email.
- [ ] (Later) Same behaviour on report sign-in modal.

---

### 12. GPS / current location accuracy

**Status:** **Shipped** (v1.0.3).

**Problem:** The map sometimes shows a poor or stale GPS fix. The home strip shows `GPS accuracy: ±Xm` or “searching…”, but pin position can still feel wrong or jump.

**Implemented:**

- [x] Report screen shows `GPS ±Xm` when using device GPS (green / amber / red by threshold).
- [x] **Warn** when accuracy > **50 m** — submit still allowed; amber copy suggests retry or manual entry.
- [x] **Block** auto-GPS submit when accuracy > **100 m** — footer “GPS TOO INACCURATE — RETRY OR MANUAL”; manual Maps URL / coordinates bypass check.
- [x] Existing calibrated location pipeline unchanged (`fetchBestCalibratedLocation`).

**Files:** `NewReportScreen.kt` (`LocationBlock`, `GPS_SUBMIT_*_ACCURACY_METERS`).

**Still optional later:** “Improving location…” extended wait for better fix before first display (`HomeScreen.kt` passive pin).

---

### 13. Report submit loading feedback

**Status:** **Shipped** (v1.0.2).

Submit button shows `SUBMITTING…` + spinner; form locked during save/upload.

**Files:** `NewReportScreen.kt`.

---

### 14. More map area labels (zoom-based show / hide)

**Status:** **Shipped** (v1.0.6) — basemap labels. Seeded lat/lon overlay removed.

**Decision:** Use CARTO **`light_all`** tiles for place / street / water names. No curated coordinate seed. Heat, clusters, and city outline stay custom overlays on top. Label readability via **per-tile bitmap remap** (`CartoBasemapLabelDarkener`) — mid-grey label ink only; no ColorMatrix (that was fading / thinning streets). Useful max zoom capped at **18** so roads are not lost to overscaled tiles.

**This pass:**

- [x] Home map tile source → `light_all`
- [x] Removed `MapPlaceLabels.kt` and custom seeded label overlay
- [x] Heat / clusters / GPS / outline untouched
- [x] Darker labels via per-tile mid-grey remap (home + report detail); ColorMatrix removed
- [x] Max zoom capped at 18 so residential streets stay visible at closest zoom
- [ ] Visual smoke test (label density vs heat readability)
- [ ] Optional later: toggle nolabels vs light_all if heat feels crowded

**Files:** `CartoBasemapLabelDarkener.kt`, `HomeScreen.kt`, `ReportDetailDialog.kt`.

#### Still deferred

| Option | What |
|--------|------|
| Nudge label darken strength | Tune mid-grey scale if names still light |
| Restore zoom 19 | Only if CARTO street detail is sharp enough without upscaling |
| Custom overlay again | Only if product needs names CARTO doesn't show |
| **JSON / OSM vector labels** | Full control without tiles |
| Heat-friendly label dimming | If `light_all` fights ward colors |

**Problem (original):** Users want neighborhood / street / water names when zooming.

**Current behaviour:** Names come from the basemap provider (dynamic with zoom/pan), not app-seeded coordinates.

---

### 15. Map jumps to current location without user intent

**Status:** **Shipped** (v1.0.2).

Passive GPS updates move the pin only; camera moves on **Locate me** or city change.

**Files:** `HomeScreen.kt`.

---

### 16. Map severity filter should survive map pan / zoom

**Status:** **Shipped** (v1.0.2). Replaced “Active critical reports” link with **Severity** dropdown (All / Minor / … / Critical); filter persists during pan/zoom.

**Files:** `HomeScreen.kt` (`mapSeverityFilter`, `ReportAndTrackSection`).

---

### 17. GBA / area heat map on city map (major)

**Locked product rule (July 2026):** Heat map is **area-wise choropleth** — GBA ward polygons + density fill by report count (same severity filter as clusters). Clusters stay on top. **Color-only for MVP (17.1).** Tap + name popup (17.2) is the **next enhancement**, not required to ship heat.

**Status:** **17.1 shipping in v1.0.5** (color heat + Heat toggle on Bengaluru). **17.2 deferred.**

**Product direction (decided in discussion):**

| Decision | Detail |
|----------|--------|
| Visual style | **Ward-style choropleth** (filled polygons, pale → deep red by report count) — sample looked good |
| Clusters | Numbered red disks **unchanged** — heat draws **under** clusters |
| Severity | Heat uses the **same severity filter** as clusters |
| Geometry for MVP | Official **369 GBA wards** as fill geometry (data already in app) — **do not draw ward numbers** on the map |
| User orientation (MVP) | Place names via **G-lite labels** + color legend only |
| Tap / name popup | **Next enhancement (17.2)** — Namma Kasa–style highlight + area name/count card |
| Named sectors (HSR Sector 1, BTM Phase 2) | **Later** — better for citizens, needs curated polygons; optional after ward choropleth |
| Soft blob / grid heat | Alternative if choropleth feels too busy; lower priority than ward style |

#### 17.1 — Choropleth heat layer (core) — ship this first

- [x] **Heat** toggle top-right, in line with **Severity** (Bengaluru only; default **off**; stays on across zoom/locate)
- [x] Fill ward polygons by report count under current severity filter
- [x] Soft/light ward boundary strokes (not strong red)
- [x] Cluster disks: **white + black count** while heat on; **red fill** while heat off
- [x] Clip / draw only visible wards; Bengaluru first
- [ ] Legend (“Reports by area”) — **deferred enhancement**
- [ ] Performance check on mid-range phones with 369 polys

#### 17.2 — Interactive area highlight + name popup (next enhancement)

- [ ] Tap area → highlight outline + card with ward name, corporation, report count
- [ ] Clusters remain visible on top / taps do not fight cluster interaction
- [ ] Optional: map ward → friendlier locality title (HSR / BTM) when available
- [ ] Optional: Fewer → More legend chip on map

**Files:** `AreaDensityHeatOverlay.kt`, `BengaluruGbaWards.kt`, `HomeScreen.kt` (`OsmDensityMap`).

**Acceptance criteria (17.1):**

- [x] Heat toggle above map (not on-map FAB); off restores red clusters
- [x] Severity filter drives fill intensity
- [x] Heat stays on through zoom / locate / pan until user toggles off (or leaves Bengaluru)
- [x] Color-only — no tap required
- [x] Inside GBA / Bengaluru only

---

### 35. Weather rain-risk map overlay (Open-Meteo → radar Phase C)

**Ask:** Show where rain is likely on the city map (helps drivers anticipate flooded / worsened pothole stretches). Complements Heat (report density) without mixing fills.

**Status:** **Phases A+B in code** (Aug 2026). **Blob patches (18 Aug 2026) — device-verify before more threshold tweaks.** **#35.1b is the next important map enhancement** (light/partly cloud still visible). **Phase C** (live radar / nowcast + trip cue) stays after that.

**Locked product rules:**

| Decision | Detail |
|----------|--------|
| Toggle | **Weather** chip next to Heat / Severity (default **off**) |
| Horizons | Chips **Now · 1h · 3h · 6h · 12h · 24h**. **Now** = current hour. Other chips = forecast at that clock time. Map wash still uses rain/cloud in the window from now. |
| Visual | Soft **cloud ovals** over metro cells, **hue by intensity** (grey cloud → violet drizzle → cyan light → yellow-green moderate → orange-red heavy → magenta storm). Not street-perfect. |
| Exclusivity | Weather **XOR** Heat — turning one on turns the other off |
| Trip cue | **Deferred to Phase C** — only after overlay is solid |
| Data (A+B) | Open-Meteo multi-point forecast (no API key); ~4×4 cell grid; cache ~10 min |

#### 35.1 — Phase A+B (shipped in code)

- [x] Extend Open-Meteo for hourly + daily metrics (`CityWeatherRepository.fetchRainGrid`)
- [x] Soft rain-risk overlay on MapLibre (`LYR_WEATHER`) + osmdroid (`CityWeatherRainOverlay`); intensity hue (Aug 2026)
- [x] Weather toggle + Now / 1h / 3h / 6h / 12h / 24h chips; Heat mutual exclusion
- [x] Hide overlay on fullscreen trip (same gate pattern as Heat)
- [x] Keep home weather subline (`fetchToday`) unchanged
- [x] Localized cloud blobs per cell (not a city-wide rectangle) — **18 Aug 2026; needs device test**

**Key files:** `CityWeatherRainGrid.kt` (`CityWeatherRainColors`), `CityWeatherRepository.kt`, `CityWeatherRainOverlay.kt`, `MapLibreDensityMap.kt`, `NeighbourhoodMapFilters.kt`, `HomeScreen.kt`.

#### 35.1b — Device verify + light / partly cloud (next important map enhancement)

**Do not lower thresholds until a Map-tab test run proves blobs actually draw.** Earlier MapLibre fills were invisible (hidden FillLayer, no repaint, pause/resume). If the test shows patches, then paint **mainly clear / thin cloud** too — not only ≥22% cover.

**Test run (do first):**

- [ ] Map tab → Weather on → 1h, then 3h / 6h / 12h / 24h
- [ ] Confirm ovals appear and grow with a longer window
- [ ] Partly cloudy ticker: scattered grey patches, not a full-city sheet
- [ ] Clear window: little or nothing is OK
- [ ] Leave Map and return: wash still there

**Then (if patches work but look too empty):**

- [ ] Paint cells below the current ~22% cloud gate (mainly clear / thin cloud)
- [ ] Keep rain cells on the intensity palette; keep Heat XOR Weather
- [ ] Do **not** add extra FillLayers (GPU crash history) — stay on one GeoJSON fill + feature colors

**Reminder trigger:** After any Map / Weather turn, or when they ask “what next?” after #36.1–36.5. Do not let 35.1b go silent behind Phase C radar.

#### 35.2 — Phase C (enhancement — after 35.1b)

**Ask:** Live / near-term “where is rain *now*” plus optional in-trip awareness.

| Track | Work |
|-------|------|
| **C1 — Radar / nowcast tiles** | Animated or stepped precipitation radar over MapLibre (provider TBD: RainViewer-style, OpenWeather, Mappls/IMD commercial, etc.). Respect ToS, API keys, and tile caching. |
| **C2 — Accuracy copy** | Honest UI (“area tendency / radar ~km scale” — never claim street-perfect). |
| **C3 — Trip rain cue** | After overlay is solid: soft chip / TTS-safe line e.g. “Heavy rain likely on this route in ~40 min” (mute-respecting; no spam). |
| **C4 — Product gates** | Weather vs Heat vs future Traffic (#31 B) — one fill at a time; Settings kill-switch if needed. |
| **C5 — Point-in-time forecast (parked)** | Optional second mode: treat `1h / 3h / 6h / 12h / 24h` as a **future clock time** (“what does rain look like around 3 hours from now”) instead of today’s default **window from now** (“rain sometime in the next 3 hours”). Not required now — keep duration-from-now as the product meaning. |

**Depends on:** Stable A+B field verify; chosen radar provider + cost; trip mute / alert scheduler patterns (#22 / #29).

**Acceptance (Phase C draft):**

- [ ] Radar/nowcast toggle or mode under Weather (or separate chip) without stacking on Heat
- [ ] Visible rain cells update on a sensible refresh (e.g. 5–15 min) without janking the map
- [ ] Optional trip cue only when confidence/horizon is useful; respects mute
- [ ] Privacy / Data safety updated if a new third-party weather/radar endpoint is added

---

### 36. Home / hub / map polish (next run — do first)

**Ask (14 Aug 2026):** Complete these together. **36.6–36.9 shipped** the same day; **36.2 and 36.3 shipped** later. **36.1, 36.4–36.5** remain.

**Status:** **Partial.** **36.2, 36.3, 36.6–36.9 shipped.** **36.1, 36.4–36.5 queued.**

| # | Work | Notes |
|---|------|--------|
| **36.1** | **Report type per hub tile** | Each Report hub type (pothole, manhole, light pole, waterlogging, reckless driving, harassment, stray dog, …) must submit / store / display as **that** civic type end-to-end — not a generic pothole-shaped report. NewReport, My Reports, map filter, and hub entry must stay type-aware. |
| **36.2** | **View all → bar chart + list** | **Shipped.** Home **All Issues → View all** opens List / Bar / Pie + Sort (accountability charts), not the issue map. |
| **36.3** | **Speed breakers are not dots** | Confirm as speed breaker → **yellow-black strip 90° to the road**. Personal until **5** users confirm the same spot, then everyone sees it. Never a numbered cluster dot. |
| **36.4** | **Unregistered civic marks follow the filter** | Unregistered **pothole** must **not** be a generic dot. When **Potholes** is selected, count/show it with potholes. When **Open manholes** (or other type) is selected, count/show that type — same rule per selected issue filter. |
| **36.5** | **Map page look** | Issue Map / Map tab chrome does not look good — restyle layout, filters, markers, and empty states so it matches the neighbourhood UI. |
| **36.6** | **SOS capsule + unread Alerts dots** | **Shipped.** Home bell replaced by SOS capsule. Unread red dot on the Alerts tab (not SOS). Each unread Alerts row keeps a red dot until that item is read. |
| **36.7** | **Dark theme contrast** | **Shipped.** Help / About / Settings / Profile / Notifications dialogs use a light surface so `CitizenInk` copy stays readable in system dark theme. |
| **36.8** | **Help and Support** | **Shipped.** Clickable `citygridhelp@gmail.com` (`mailto:`) plus How-to videos (one row per app section; YouTube channel placeholder until dedicated videos exist). |
| **36.9** | **About — weekend PDF** | **Shipped (copy only).** About states that every weekend a PDF is emailed to the user and the government covering concerns + city-wide issue types. No PDF generator yet. |

**Key files (36.6–36.9):** `NeighbourhoodHomeScreen.kt`, `NeighbourhoodShell.kt`, `NeighbourhoodBottomBar.kt`, `NeighbourhoodAlertsScreen.kt`, `NeighbourhoodTrustScreen.kt`, `HomeScreen.kt` (About), `CityGridSupport.kt`, `Theme.kt` (`CitizenLightSurface`).

**Acceptance (draft):**

- [ ] Hub tile → NewReport uses that `CivicIssueType`; saved report keeps it; filters/lists match
- [x] View all opens a bar chart + list of recent updates (not only the map)
- [x] Speed-breaker marks (personal or public) render as a yellow-black strip across the road (not a cluster dot)
- [ ] Issue filter: unregistered potholes count with potholes; unregistered manholes with manholes; no leftover anonymous dots for those kinds
- [ ] Map tab / Issue Map visual polish matches home (spacing, chips, markers, empty/loading)
- [x] Home header shows SOS capsule instead of bell; Alerts tab gets the unread red badge
- [x] Each unread alert row keeps a red dot until that item is read
- [x] About City Grid (and other Profile/About/Help strings) readable in dark theme
- [x] Help: `citygridhelp@gmail.com` mailto + YouTube how-to links per section
- [x] About copy mentions weekend PDF emailed to user and government covering concerns + city issue report

---

### 18. “Report a pothole” CTA — rounded button, centered label

**Status:** **Shipped** (v1.0.2). Pill/capsule shape on home CTA and New Report submit button.

**Files:** `HomeScreen.kt`, `NewReportScreen.kt`.

---

### 19. Tap map cluster numbers → view pothole reports (testing / exploration)

**Status:** Not implemented — enhancement backlog (July 2026). **Implement last** (after core fixes and #17 heat map).

**Problem (testing feedback):** On the home map, **numbered red cluster disks** show how many reports are in an area, but **tapping a number does nothing**. Testers want to **open and inspect** those reports from the map.

**Current behaviour:** `ReportClusterMarkersOverlay` in `HomeScreen.kt` only **draws** circles + counts (`ReportGeoCluster` has `lat`, `lon`, `count` only — no report IDs). No hit-testing or click handler. Report detail already exists via `ReportDetailDialog` from **My Reports** / recent list, not from map clusters.

**Product notes:**

- Public map feed is **anonymous** — detail view must not expose reporter identity beyond what recent reports already show.
- Cluster may represent **multiple** reports at one disk — need UX for `count > 1`.

**Ideas (pick one or combine when implementing):**

| Idea | Behaviour | Pros | Cons |
|------|-----------|------|------|
| **A. Bottom sheet list** | Tap cluster → sheet lists reports (thumbnail, severity, status, date, street/area); tap row → `ReportDetailDialog` | Clear for testers; works for any count | Two taps to full detail |
| **B. Zoom to split** | Tap cluster with `count > 1` → zoom in and re-cluster; tap `count == 1` → detail | Natural map exploration | Extra work; may need several taps |
| **C. Direct detail (single)** | If `count == 1`, open `ReportDetailDialog` immediately | Fast for sparse areas | Needs report ID on cluster |
| **D. Carousel / pager** | Tap cluster → horizontal photo cards at bottom, swipe between reports in cluster | Visual, good for demos | More UI work |
| **E. Spiderfy** | Tap cluster → fan out pins around center, each pin opens detail | Common map pattern | Busy on small screens |

**Recommended for City Grid (when picked up):** **A + C** — bottom sheet for multi-report clusters; single-report clusters open detail directly. Optionally add **B** on second tap of same cluster.

**Technical prerequisites:**

- [ ] Extend clustering to retain **report IDs** per cluster (not just count + centroid).
- [ ] Hit-test tap on cluster overlay (or replace with tappable `Marker`s / custom `Overlay.onSingleTapConfirmed`).
- [ ] Reuse `ReportDetailDialog` / `PersistedPotholeReport` from `RecentReportsRepository.reportsForMapInMetro`.
- [ ] Respect **active critical** filter — only list critical reports when that mode is on.

**Files:** `HomeScreen.kt` (`buildReportRadiusClusters`, `ReportClusterMarkersOverlay`, `ReportGeoCluster`), `ReportDetailDialog.kt`, possibly new `ClusterReportsBottomSheet.kt`.

**Acceptance criteria:**

- [ ] Tap on visible cluster number opens report(s) for that cluster.
- [ ] Multi-report clusters show a list; user can open full detail.
- [ ] No new PII on public map beyond existing recent-reports rules.
- [ ] Works in normal and **active critical** map modes.

---

### 20. Weekly pothole report digest for citizens (email / in-app)

**Status:** Not implemented — **enhancement backlog** (captured July 2026). Needs product + backend design before build.

**Idea:** Once a week, each interested citizen gets a short **city / GBA performance digest**:

| Digest section | Content (draft) |
|----------------|-----------------|
| Volume | Total potholes **reported** in the period (city and/or their wards of interest) |
| Closure | How many were **closed / completed** by BBMP / GBA |
| ETA | **GBA average ETAs** (e.g. open → in progress, open → completed) where data exists |
| Rankings | **Ward-wise rankings** — which ward areas are doing better (closure rate, speed, backlog) |

**Channels (decide later):**

- Email to signed-in users (opt-in preferred)
- And/or in-app bell / Accountability card summarizing the same week

**Dependencies / open questions:**

- [ ] Aggregate stats from `reports` (status transitions, `ward_*`, corp, timestamps) — may need server-side job + RLS-safe summary tables
- [ ] Define “closed by BBMP/GBA” precisely (status = completed only? exclude citizen-cancelled?)
- [ ] ETA definitions and enough completed tickets for stable averages
- [ ] Ranking formula (closure %, median days open, reports per ward, fairness for small wards)
- [ ] Opt-in / privacy — city-wide aggregates only; no other citizens’ PII
- [ ] Cadence: weekly which day/time (IST); first digest after enough live data

**Not in scope for first cut:** Officer-facing dashboards (gov app); real-time live rankings on every home open.

**Reminder trigger:** After 1.0.5+ heat/labels settle and there is steady Bengaluru report volume for meaningful weekly stats.

---

### 21. Accountability — city / ward overview toggle (beyond “my reports”)

**Status:** Not implemented — **enhancement idea** (captured July 2026). **Product still to be refined** before design/build.

**Today:** Accountability shows the signed-in user’s **own** reports and municipal routing details.

**Enhancement idea:** Add a **toggle** on Accountability so the user can switch between:

| Mode | Intent (draft) |
|------|----------------|
| **My reports** (current) | Personal tickets + status |
| **City / ward overview** | Broader view: wards (or corps), report counts, solved vs pending, how long pending, etc. |

**Draft metrics for overview mode (to decide):**

- Reports **open / in progress / completed** per ward (or corporation)
- **Pending age** (e.g. oldest open, or average days open)
- Optional link to map heat / ward context

**Open product questions (user wants to think more):**

- [ ] Scope: all GBA vs user’s corporation vs nearby wards only?
- [ ] Privacy: aggregates only — never other reporters’ names/photos in this tab?
- [ ] Does overview replace Accountability copy or sit beside personal list?
- [ ] Performance with 369 wards — list, top-N, or search?
- [ ] Overlap with **#20** weekly digest and **#17** heat map — avoid three competing “city score” UIs

**Reminder trigger:** After weekly digest (#20) direction is clearer, or when testers ask for city-wide Accountability beyond personal tickets.

---

### 22. Trip navigation — ahead pothole alerts on route

**Status:** **In progress** (v1.1.0 target) — Phase 1–3 implemented in app; run Supabase migration `0012_report_direction_metadata.sql` before sync.

**Behaviour:**

| Piece | Detail |
|-------|--------|
| Report metadata | Lane position (L/M/R), device bearing, wide-shot traffic direction → stored on report |
| Trip toggle | Home map **Trip** chip (outside map frame); disables **Heat** while active |
| No destination | Ahead **cone** along GPS bearing (~35° half-angle, 2.5 km) |
| With destination | Start + Destination search on map → OSRM route; alerts within **28 m** corridor, **2.5 km** ahead |
| Vehicle | Car / Bike chips on planner panel |
| Lane side label | Shown when GPS accuracy ≤ **25 m** and report has traffic bearing |
| Fullscreen map | In-composition expand/collapse (not a Dialog — Dialog was sending the app to background) |

**Key files:** `TripNavigationMatcher.kt`, `OsrmRouteClient.kt`, `TripNavigationOverlay.kt`, `ReportDirectionMetadata.kt`, `NewReportScreen.kt` (`TrafficDirectionConfirmRow`), `HomeScreen.kt`, `0012_report_direction_metadata.sql`.

**Open / follow-up:**

- [ ] Apply migration `0012` on Supabase
- [ ] Smoke test on device (bearing updates, OSRM offline fallback)
- [ ] Optional: show lane/traffic metadata in `ReportDetailDialog`
- [ ] Play Store release notes for trip feature
- [ ] **Parked — in-nav trip banner:** restore bottom nav card (remaining distance, ETA %, next turn, ahead pothole line). Removed from live UI to free the map; replace with a new bottom control when designed. Logic helpers still in `HomeScreen.kt` (`remainingTripSeconds`, `formatTripEta`, `formatTripDistance`, turn/alert state).
- [x] **Parked — responsive map chrome:** scale trip/map chrome (FABs, compass, GPS chip, search fields, fonts) from screen width so small phones stay compact; keep labels single-line (ellipsis, no wrap). **`MapUiScale` / `rememberMapUiScale`** wired into osmdroid + MapLibre chrome, planner, nav rail (Aug 2026).
- [x] Street-follow POV + speed-linked zoom polish — MapLibre speed bands + look-ahead; smoother OSM zoom lerp (Aug 2026).
- [x] Offline nav: GPS progress continues; offline chip; map/info refresh when online (Aug 2026).
- [ ] Pending-upload UX message when network is slow during report submit

---

## How to use this list

### Release roadmap (July 2026)

| When | Focus | Items |
|------|--------|-------|
| **★ Next run** | **#36** remaining: hub types · unregistered filter · map look | **#36.1, 36.4–36.5** |
| **★ Next map enhancement** | **#35.1b** Weather — device-verify blobs, then light/partly cloud | After a Map-tab test run |
| **★ Now** | **#34** re-enable bump / companion / USB after testing | **#34.1–34.3** (`TripFeatureGates`) |
| **★ Now** | **ML Phase H** hardening (optional) | **#33** H |
| **Major / periodic** | Indian YOLO retrain + heuristics→YOLO + Supabase photo loop | **#33M** |
| **After a few steps** | **#31** navigation map enhancement | Live + journey-start traffic (Mappls/TomTom; crowd later) |
| **Enhancement (map)** | **#35.2** Weather Phase C — radar / nowcast + trip rain cue | After **#35.1b** |
| **Parked** | Numbered enhancements #23–#30 etc. | Resume as needed |
| **Later (citizen)** | **#20** weekly digest; **#21** Accountability overview toggle | Needs product / device matrix |
| **Last (citizen)** | Testing polish | **#19** tap cluster → view reports |
| **Parked** | Government app | **#9**, **#10.2** seed — [government_app_handover.md](government_app_handover.md) § G1–G8 |
| **Low / strategic** | Identity | #2 account linking, #3 email history |

### Priority reference

| Priority | Suggested order |
|----------|-----------------|
| **★ Next run** | **#36.1, 36.4–36.5** hub types · unregistered issue filter · map look (**36.2, 36.3, 36.6–36.9 shipped**) |
| **★ Next map enhancement** | **#35.1b** Weather device-verify + light/partly cloud (important; do not skip) |
| **★ Now** | **#34** bump · companion · USB — finish testing, flip `TripFeatureGates` |
| **★ Now** | **#33 Phase H** hardening (optional) when field-ready |
| **Major / periodic** | **#33M** Indian YOLO retrain · heuristics→YOLO · Supabase training loop |
| **After a few steps** | **#31** Live traffic + journey-start ETA (navigation map enhancement) |
| **Enhancement (map)** | **#35.2** Weather Phase C — live radar / nowcast + optional trip rain cue (**after 35.1b**) |
| **Parked (enhancements)** | **#23–#30**, #17.2, #19–#21, #25.3, #26 |
| **Parked (gov)** | **#9** two-way sync, **#10.2** seed/officers — [government_app_handover.md](government_app_handover.md) |
| Low / strategic | #2 account linking, #3 email history |
| Reference only | #1 (document current design) |
| **Shipped (v1.0.2–1.0.6 + backend)** | #4, #5, #6, **#8**, #11, #12, #13, #14 (G-lite → basemap), #15, #16, #17.1, #18, **#10.0–10.1, #10.4–10.5** GBA · trip #22–#28 · **#32** (phone path live; companion/USB gated **#34**) · **#33 Phase A–G** (bump gated **#34.1**) · **#35.1** Weather A+B (rain-risk grid; Phase C deferred) |

When an item ships, follow **After each implementation — update checklist** at the top of this file. Minimum: mark the item **Shipped** with version + update the **Shipped** row in the tables below.

**Product thresholds & defaults (duplicate radius, GPS, map clusters, auth, etc.):** see [product_assumptions.md](product_assumptions.md).

**Pothole photo classification (models, features, thresholds, metrics):** see [pothole_classification_model.md](pothole_classification_model.md).  
**Full ML pipeline & risk analyzer:** see [ml_models_and_evaluation.md](ml_models_and_evaluation.md).
