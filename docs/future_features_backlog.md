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
| **2** | Update **`officers.json` / Supabase seed** with GBA commissioners | **Parked** — gov backlog **G3**; placeholder commissioners in seed until verified | **Parked** |
| **3** | **Re-map existing reports** (optional backfill) or **new reports only** | **Forward-only** — historical rows keep old 8-zone `assignee_key` | **Decided** |
| **4** | **Accountability UI labels** — “East Corporation” vs “East Zone” | `AccountabilitySection.kt` GBA copy | **Shipped** (v1.0.3 dev) |

**Suggested implementation order:** Phase **1 → 2 → 4** in one release; Phase **3** decided before seed deploy (backfill vs forward-only).

**Depends on:** Verified commissioner names from BBMP/GBA public directory.

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

**Status:** **In progress — Google-like tiers + denser Bengaluru names** (post-1.0.5 / next store cut). G-lite shipped in v1.0.4.

**Decision (July 2026):** Keep CARTO `light_nolabels` + custom overlay. **#14 now adds explicit tiers** matching Google zoom-out order: **STREET → LANDMARK → LOCALITY → DISTRICT → MACRO**. True full street-network GIS / labeled tiles still deferred.

**Shipped in G-lite (v1.0.4):**

- [x] **`maxZoom` tiering** — macro labels hide when zoomed in
- [x] **`priority`** for grid tie-break
- [x] Reduced `labelRevealSlack`
- [x] Extra localities + corridor names

**Added in #14 (this pass):**

- [x] **`MapLabelTier`** enum: MACRO / DISTRICT / LOCALITY / LANDMARK / STREET
- [x] Tier-driven default `maxZoom` + zoom-out priority (streets drop first)
- [x] Tighter reveal slack (more Google-like)
- [x] **~250 Bengaluru labels** including Kasavanahalli / Haralur / HSR sectors, Bren Imperia + nearby landmarks, more streets
- [ ] Play smoke test at SE Bangalore zoom ladder (street → landmark → locality)
- [ ] Optional further bulk names if testers still report blank pockets

**Files:** `HomeScreen.kt` (`MapLabelTier`, `RegionLabelSpec`, `RegionNameLabelsOverlay`, `bengaluruRegionLabelSpecs`).

#### Still deferred

| Option | What |
|--------|------|
| **G-full overlay** | Bbox collision, fade persistence |
| **JSON assets** | Move specs out of Kotlin |
| **E — Labeled tiles** | CARTO `light_all` / OSM labels |
| Full street GIS | Every lane name without vector tiles |

**Problem (original):** Users want **more neighborhood / area names** as they **zoom in**, and fewer when **zoomed out** — without cluttering the city overview.

**Current behaviour:** Tiered Bengaluru labels with grid de-overlap; other metros still use minZoom-only lists.

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

## How to use this list

### Release roadmap (July 2026)

| When | Focus | Items |
|------|--------|-------|
| **Now** | Ship **1.0.5** to Play closed test | Area heat map (#17.1); resume fix; Play update bell |
| **Next (citizen)** | **#14** Google-like label tiers; optional **#10.6** | After 1.0.5 |
| **Later (citizen)** | **#17.2** area tap + name popup | Deferred after heat color |
| **Later (citizen)** | **#20** weekly digest; **#21** Accountability overview toggle | Needs product design |
| **Last (citizen)** | Testing polish | **#19** tap cluster → view reports |
| **Parked** | Government app | **#9**, **#10.2** seed — [government_app_handover.md](government_app_handover.md) § G1–G8 |
| **Low / strategic** | Identity | #2 account linking, #3 email history |

### Priority reference

| Priority | Suggested order |
|----------|-----------------|
| **Next (citizen)** | **#14** Google-like label tiers; optional **#10.6** corp reference UI |
| Shipping now | **#17.1** GBA heat map (color) — **1.0.5** |
| Later (citizen) | **#17.2** area tap + name popup + heat legend |
| Later (citizen) | **#20** weekly report digest; **#21** Accountability city/ward overview toggle *(design TBD)* |
| Last (citizen) | **#19** tap map cluster → view reports |
| **Parked (gov)** | **#9** two-way sync, **#10.2** seed/officers — [government_app_handover.md](government_app_handover.md) |
| Low / strategic | #2 account linking, #3 email history |
| Reference only | #1 (document current design) |
| **Shipped (v1.0.2–1.0.4 + backend)** | #4, #5, #6, **#8**, #11, #12, #13, #14 G-lite, #15, #16, #18, **#10.0–10.1, #10.4–10.5** GBA boundary/corp/ward routing, map bounds/clusters/severity/GPS pill |

When an item ships, follow **After each implementation — update checklist** at the top of this file. Minimum: mark the item **Shipped** with version + update the **Shipped** row in the tables below.

**Product thresholds & defaults (duplicate radius, GPS, map clusters, auth, etc.):** see [product_assumptions.md](product_assumptions.md).

**Pothole photo classification (models, features, thresholds, metrics):** see [pothole_classification_model.md](pothole_classification_model.md).  
**Full ML pipeline & risk analyzer:** see [ml_models_and_evaluation.md](ml_models_and_evaluation.md).
