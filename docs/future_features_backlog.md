# Citizen app — future features backlog

Planned or deferred work for the City Grid citizen app (`potholereport`). Items here are **not** in the current release scope unless marked as shipped.

For release steps and what is already live, see [release_and_versioning_guide.md](release_and_versioning_guide.md).

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

**Status:** Partially shelved (was in IDE shelved patches).

Validate that report GPS / picked map location lies inside the selected city metro before submit, with clear citizen-facing errors.

---

## UI & notifications

### 6. In-app notifications bell

**Status:** Not implemented (shelved patch).

Header bell for citizen notifications (e.g. app update prompts, report status messages) wired to `CitizenNotificationsRepository`.

---

### 7. Email verification settings tab (admin / template polish)

**Status:** Partially deferred.

Signup and profile email change rely on Supabase email templates using `{{ .Token }}` (Confirm signup, Change email address) and **Secure email change off** for a single-code profile update. Operator checklist: [`docs/supabase_email_templates.md`](supabase_email_templates.md).

---

### 8. Report submission confirmation email

**Status:** Implemented in repo — **deploy** migration `0010`, Brevo secrets, Edge Function, and Database Webhook.

After a **signed-in** citizen’s report is inserted into `public.reports`, a webhook
calls `notify-citizen-report-created` to email the citizen via Brevo.

| Item | Decision |
|------|----------|
| Ticket in email | `CG-` + last 8 digits of `reports.id` |
| User ID in email | Raw `reporter_user_id` (`PW-xxx`) |
| Timestamp | IST |
| Idempotency | `report_email_log` |

Deploy guide: [`docs/report_confirmation_email.md`](report_confirmation_email.md).

---

## Government / cross-app

### 9. Deeper two-way sync with CG GOVT app

**Status:** Ongoing / handover.

Citizen push to `reports` + `evidence` is implemented. Full round-trip (all status transitions, proof photos visible in citizen My Reports) depends on government app workflows and migrations — see [government_app_handover.md](government_app_handover.md).

---

### 10. Bengaluru municipal areas — BBMP zones / GBA corporations / wards

**Status:** Not implemented — **remind after internal testing** before wide Bengaluru pilot.

**Problem:** Citizens may feel “too few BBMP areas” in the app. Today routing uses **all 8 classic BBMP zones** (complete for that model), but not **198/369 wards** or the **5 GBA corporations (2025)**.

**Full draft (options A–D, phasing, files, acceptance criteria):**  
[`docs/future_govt_bbmp_gba_zones.md`](future_govt_bbmp_gba_zones.md)

**Suggested path:**

| Phase | What |
|-------|------|
| 1 | Zone reference UI + clearer Accountability copy (Option A) |
| 2 | Migrate to 5 GBA corporations + officer seed (Option B) |
| 3 | Ward polygons + ward officers if GBA data available (Option C) |
| Parallel | More map locality labels via JSON (Option D) |

**Reminder trigger:** When starting BBMP/GBA official pilot, or when stakeholders ask for Central/North corporation or ward-level routing.

---

### 11. Signup interrupted after “Send verification code” (stuck / “email already exists”)

**Status:** Not implemented — **discussed, not coded** (June 2026). **Review / implement tomorrow.**

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

**Status:** Not implemented — enhancement backlog (June 2026).

**Problem:** The map sometimes shows a poor or stale GPS fix. The home strip shows `GPS accuracy: ±Xm` or “searching…”, but pin position can still feel wrong or jump.

**Likely causes to investigate:**

- Single-shot / last-known location used before a fresh fix completes (`fetchBestCalibratedLocation`, `requestSingleUpdate` in `NewReportScreen` / `HomeScreen`).
- Indoor / weak signal; no fusion with network location where helpful.
- GPS pin on map vs report submit location using different pipelines.
- No clear “low accuracy” warning before submit when `accuracy` is poor (e.g. > 50 m).

**Rough scope:**

- [ ] Prefer best available fix (fresh GPS, then network, with timeout) before tagging a report.
- [ ] Show accuracy on report screen and block or warn submit when accuracy exceeds a threshold (configurable).
- [ ] Optional: short “improving location…” state while waiting for a better fix.
- [ ] Re-test on physical device outdoors vs indoors.

**Files:** `HomeScreen.kt` (GPS pipeline, `gpsPinLocation`), `NewReportScreen.kt` (`refreshGpsLocation`, `latEffective` / `lngEffective`).

---

### 13. Report submit loading feedback

**Status:** Not implemented — enhancement backlog (June 2026).

**Problem:** Tapping **Submit report** can take noticeable time (save photos locally, optional on-device checks, `RecentReportsRepository.addReport`, `ReportSyncRepository.pushReport`) with no dedicated loading UI — button stays as “SUBMIT REPORT” and the screen may feel frozen.

**Rough scope:**

- [ ] `submitting` state: disable form, show progress indicator on button (e.g. `SUBMITTING…`) or full-screen / inline overlay.
- [ ] Optional branded animation (City Grid) for longer sync paths.
- [ ] Prevent double-tap submit.
- [ ] Clear error path if IO/sync fails (keep form open, as today).

**Files:** `NewReportScreen.kt` (submit `Button` + `scope.launch` block ~line 720).

---

### 14. More map area labels (zoom-based show / hide)

**Status:** Not implemented — enhancement backlog (June 2026).

**Problem:** Users want **more neighborhood / area names** as they **zoom in**, and fewer (or none) when **zoomed out** — without cluttering the city overview.

**Current behaviour:** `bengaluruRegionLabelSpecs` (~200 labels) in `HomeScreen.kt` with per-label `minZoom`; many names only appear at high zoom. Coverage may still feel sparse in some areas.

**Rough scope:**

- [ ] Audit missing Bengaluru (and other metro) localities vs user feedback.
- [ ] Move label specs to `assets/` JSON (see also `future_govt_bbmp_gba_zones.md` Option D).
- [ ] Tune `minZoom` / optional `maxZoom` per label so labels **appear on zoom in** and **hide on zoom out**.
- [ ] Performance check with OSMDroid `RegionLabelsOverlay` at high label counts.

**Files:** `HomeScreen.kt` (`RegionLabelSpec`, `bengaluruRegionLabelSpecs`, `regionLabelSpecsForCity`).

---

### 15. Map jumps to current location without user intent

**Status:** Not implemented — enhancement backlog (June 2026).

**Problem:** While viewing **city overview** or a manually panned area, the map sometimes **auto-moves to the user’s GPS pin** without tapping **Locate me**.

**Likely causes to investigate:**

- `mapLocateEpoch` / `LaunchedEffect(mapLocateEpoch)` re-centering map on GPS updates.
- Background GPS refresh updating `userLocation` / `gpsPinLocation` and triggering fit or pan.
- `scheduleFitCityOverview` vs street-view / city-view toggle fighting user pan.
- Compose `update` block re-applying camera when props change.

**Rough scope:**

- [ ] Distinguish **user-requested locate** (button) from **passive GPS updates** (update pin only, do not move camera).
- [ ] Remember “user has panned/zoomed” flag; suppress auto-recenter until Locate me or city picker.
- [ ] Re-test: select city → pan map → wait for GPS refresh → map should **not** jump.

**Files:** `HomeScreen.kt` (`OsmDensityMap`, `mapLocateEpoch`, `scheduleFitCityOverview`, GPS `LaunchedEffect`s).

---

### 16. “Active critical reports” mode should survive map pan / zoom

**Status:** Not implemented — enhancement backlog (June 2026). **Behaviour bug.**

**Problem:** User turns on **Active critical reports** (filters map to critical + open/in-progress). After **panning or zooming** the map, the filter **turns off** and all reports show again. User must tap the link again to re-enable critical-only view — wrong for exploring the map while filtered.

**Root cause (known):** `OsmDensityMap` calls `onMapViewControlUsed` on map gestures; parent sets `showCriticalActiveClusters = false`:

```kotlin
onMapViewControlUsed = {
    if (showCriticalActiveClusters) showCriticalActiveClusters = false
}
```

Same clear happens on **Locate me** and **city view** controls (may be intentional there).

**Rough scope:**

- [ ] **Do not** clear `showCriticalActiveClusters` on pan/zoom / generic map touch.
- [ ] Only clear when user **toggles off** the critical link, **changes city**, or explicitly chooses an action that means “exit filter” (product decision).
- [ ] Keep critical styling on cluster overlay while filter is on during pan/zoom.

**Files:** `HomeScreen.kt` (`ReportAndTrackSection`, `OsmDensityMap`, `showCriticalActiveClusters`, `onMapViewControlUsed`).

---

### 17. GBA division heat map on city map (major)

**Status:** Not implemented — enhancement backlog (June 2026). **Major.**

**Goal:** A **heat-map-style grid** over the **city boundary only** (not outside metro periphery), showing **report density per GBA division**. Divisions with **more reports** use a **stronger red gradient**; fewer reports use a **lighter red**. Toggle via a **new button below the existing City view control** on the map.

**Context:**

- Bengaluru GBA (2025): **5 city corporations** — Central, North, East, South, West (369 wards total). See [`future_govt_bbmp_gba_zones.md`](future_govt_bbmp_gba_zones.md).
- Today: point markers / clusters only; no choropleth or grid heat layer.
- Requires division **boundaries** inside city metro bbox (GeoJSON/KML from OpenCity or simplified grid cells aligned to corporations).

**Rough scope:**

- [ ] **Data:** Corporation/division polygons clipped to `cityMetroBounds` for Bengaluru (extend later for other cities).
- [ ] **Counts:** Aggregate `RecentReportsRepository` reports per division (open + in-progress + completed — product rule TBD).
- [ ] **Map layer:** OSMDroid overlay — filled polygons or grid boxes with red gradient scale (min = pale, max = deep red).
- [ ] **UI:** Button under **City view** — e.g. “Division heat map” / “GBA density”; mutually exclusive or combinable with city view (TBD).
- [ ] **Legend:** Small key showing count → color scale.
- [ ] **Backend:** May share ward/corp boundaries with #10 / GBA doc; optional Supabase table for boundaries + counts cache.

**Depends on:** Division geometry (#10 Option B/C) for accurate boxes; MVP could use rough corporation bounding boxes before full ward polygons.

**Files:** `HomeScreen.kt` (map overlays, map controls), new overlay module, `MunicipalContactsRegistry` / GBA corp definitions, possibly `RecentReportsRepository`.

**Acceptance criteria:**

- [ ] Heat view limited to **inside city periphery** (metro bbox or official outline).
- [ ] Each GBA division visible with color by relative report count.
- [ ] Toggling off restores normal marker map.
- [ ] Performance acceptable on mid-range Android phones.

---

### 18. “Report a pothole” CTA — rounded button, centered label

**Status:** Not implemented — enhancement backlog (July 2026).

**Problem:** The main **Report a pothole** control on the home tab looks like a **full-width bar**, not a button — it is stretched edge-to-edge with **square corners** and **left-aligned** text/icon.

**Current UI:** `ReportAndTrackSection` in `HomeScreen.kt` — `Card` with `fillMaxWidth()`, `height(56.dp)`, `RoundedCornerShape(0.dp)`, row layout with camera icon + title/subtitle on the left.

**Desired UI:**

- **Rounded corners** (e.g. 8–12 dp or match Material button radius) so it reads as a tappable button.
- **Label centered** horizontally (icon + “REPORT A POTHOLE” + subtitle as a centered group, or title centered with subtitle below).
- Optional: **not** full-bleed width — e.g. `fillMaxWidth()` with horizontal padding already present, or intrinsic width centered in parent (product choice).
- Keep brand red (`0xFFB74233`) and existing tap behaviour (signed-in vs guest sign-in modal).

**Rough scope:**

- [ ] Replace square `Card` styling with rounded shape (`RoundedCornerShape`) or `Button` / `FilledTonalButton` with custom colors.
- [ ] Center content in `Row` / `Column` (`Arrangement.Center`, `Alignment.CenterHorizontally`).
- [ ] Quick visual check in light theme on phone + tablet widths.

**Files:** `HomeScreen.kt` (`ReportAndTrackSection` report CTA `Card`, ~lines 693–732).

**Acceptance criteria:**

- [ ] Control clearly looks like a **button**, not a flat banner strip.
- [ ] Corners visibly rounded; primary text centered.
- [ ] Same click targets and auth flow as today.

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

## How to use this list

| Priority | Suggested order |
|----------|-----------------|
| High (product) | **#11** signup OTP interrupt, **#16** critical filter + map pan bug, **#15** unwanted map recenter |
| High (UX) | **#13** submit loading, **#12** GPS accuracy, **#18** Report a pothole button styling |
| Medium | **#6** notifications bell, **#5** city match, **#14** map area labels |
| Medium (Bengaluru pilot) | **#10** BBMP/GBA zones — [future_govt_bbmp_gba_zones.md](future_govt_bbmp_gba_zones.md) |
| Major / strategic | **#17** GBA division heat map (after #10 boundaries) |
| Last (testing polish) | **#19** tap map cluster → view reports |
| Low / strategic | #2 account linking, #3 email history |
| Reference only | #1 (document current design) |
| Shipped | #4 photo download |

When an item ships, move it to the release guide or handover doc and mark it **Shipped** here with the migration/app version.

**Product thresholds & defaults (duplicate radius, GPS, map clusters, auth, etc.):** see [product_assumptions.md](product_assumptions.md).

**Pothole photo classification (models, features, thresholds, metrics):** see [pothole_classification_model.md](pothole_classification_model.md).  
**Full ML pipeline & risk analyzer:** see [ml_models_and_evaluation.md](ml_models_and_evaluation.md).
