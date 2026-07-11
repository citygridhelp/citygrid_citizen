# Government Application Handover Notes

> **Release & versioning:** See [release_and_versioning_guide.md](release_and_versioning_guide.md) for step-by-step testing, versioning, Play Store upload, Supabase migrations, rollback, and in-app update notifications for both apps.

This document tracks the current build status and functional handover points from the citizen app (`potholereport`) for government employee workflows.

## Current Build Status

- Build target: Android (`minSdk 24`, `targetSdk 36`)
- Validation status: `assembleDebug` successful (latest session)
- Core stack: Kotlin + Jetpack Compose + OSMDroid + LiteRT/TFLite
- **Government app (CG GOVT):** separate project `Potholegovt` — shares Supabase backend with this repo

## Government officers & Supabase

CG GOVT officer accounts are **not** created from the Android app. They are managed in Supabase via the seed tooling in this repo.

### Source files

| File | What it does |
|------|----------------|
| `supabase/seed/officers.json` | Master roster: email, role, city, zone, `assigneeKey` |
| `supabase/seed/seed_officers.mjs` | Creates Supabase Auth users + `gov_officers` rows |
| `supabase/README.md` | Schema apply + seed overview |

**App-side mirrors (update when zones/names change):**

| Project | File |
|---------|------|
| Potholegovt | `MunicipalOfficersRegistry.kt` — GPS auto-routing to zone head |
| Potholegovt | `GovAuthRepository.kt` — demo logins when Supabase is off |
| potholereport (citizen) | `MunicipalContactsRegistry.kt` — citizen accountability assignee |

### When you must run `seed_officers.mjs`

Run it when:

- Setting up Supabase for the first time with government logins
- Adding or editing an officer in `officers.json`
- Resetting passwords for all seeded officers

You **do not** need it for UI-only app updates. Ticket assignees refresh when officers tap **Refresh** on the dashboard.

### How to add or change an official

1. Edit `supabase/seed/officers.json` (see field list in [release_and_versioning_guide.md §6.4](release_and_versioning_guide.md#64-government-officers--roster-seeding-and-passwords)).
2. Update `MunicipalOfficersRegistry.kt` / citizen `MunicipalContactsRegistry.kt` if zones changed.
3. From PowerShell:

```powershell
cd supabase/seed
npm install
$env:SUPABASE_URL="https://YOUR-PROJECT.supabase.co"
$env:SUPABASE_SERVICE_ROLE_KEY="<service_role>"
$env:OFFICER_PASSWORD="your-secure-password"
node seed_officers.mjs
```

4. Test login on CG GOVT with the officer email.

Re-running is safe: existing users are updated, not duplicated.

### How to change passwords

| Method | Steps |
|--------|--------|
| **All officers** | Set `$env:OFFICER_PASSWORD="..."` and re-run `node seed_officers.mjs` |
| **One officer** | Supabase Dashboard → Authentication → Users → select email → reset password |
| **Default demo** | Script default is `gov123` if `OFFICER_PASSWORD` is unset — change before production |

Never put the **service_role** key in either Android app or `local.properties` (anon key only in apps).

### BBMP roles (who governs what)

| App role | Real-world role | Example |
|----------|-----------------|---------|
| `COMMISSIONER` | Municipal Commissioner — whole city, reassign only | `commissioner.bengaluru@bbmp.gov.in` |
| `ZONE_HEAD` | Joint Commissioner — one zone, default ticket owner | `ajith.bommanahalli@bbmp.gov.in` → Bommanahalli |
| `FIELD_OFFICER` | Engineer under a zone head | `ramesh.east@bbmp.gov.in` |

**Ajith M** in BBMP data is **Joint Commissioner, Bommanahalli Zone** (`ZONE_HEAD`), not the city Municipal Commissioner (`COMMISSIONER`). Potholes in Bommanahalli should route to `BENGALURU:BOMMANAHALLI`, not to the commissioner login.

Full release checklist: [release_and_versioning_guide.md](release_and_versioning_guide.md).

## Citizen App Capabilities (Current)

- New pothole reporting with:
  - AI close-up and wide-shot validation
  - Duplicate prevention for same reporter + same location (until resolved)
  - GPS/manual coordinate tagging
  - Severity tagging (manual + AI advisory)
- Accountability routing:
  - Report assigned to nearest municipal officer by city/zone
- My Reports:
  - Status view (Open / In Progress / Completed)
  - Resolution rate stats and filters

## AI + Risk Inputs Useful for Government App

- AI photo validation summary and confidence (on-device)
- Suggested severity from pothole-risk analysis
- Estimated pothole width/depth (advisory-scale estimate)
- Suggested rider speed near pothole (risk advisory)

## Government enhancements backlog (parked)

**Strategy (July 2026):** Finish **citizen app** closed testing first. All CG GOVT work lives here until the citizen app ships **1.0.3+** and testers stabilize. Citizen master list: [`future_features_backlog.md`](future_features_backlog.md).

GBA detail (corporations, wards, officers): [`future_govt_bbmp_gba_zones.md`](future_govt_bbmp_gba_zones.md).

### Priority order (when citizen app is stable)

| # | Enhancement | Scope | Depends on |
|---|-------------|-------|------------|
| **G1** | **Consume ward routing from citizen reports** | Read `ward_key`, `ward_number`, `ward_name`, `assignee_*` on sync; show in gov ticket detail | Migration `0011` applied (citizen already pushes these) |
| **G2** | **GBA corporation registry mirror** | Update `MunicipalOfficersRegistry.kt` — 5 corp keys (`BENGALURU:GBA_*`); keep legacy 8 BBMP keys for old tickets | Citizen `MunicipalContactsRegistry.kt` (shipped 1.0.3) |
| **G3** | **Verify commissioner names + seed officers** | Edit `officers.json` with real GBA names; run `seed_officers.mjs`; test CG GOVT logins | Public GBA/BBMP directory |
| **G4** | **Two-way sync (#9)** | Status OPEN → IN PROGRESS → COMPLETED round-trip; completion proof photos visible in citizen **My Reports** | Gov workflow RPCs + citizen pull merge |
| **G5** | **Gov map — corp / ward overlays** | Choropleth or boundary lines on gov map; queue filter by corporation / ward | Ward asset `bengaluru_gba_wards.json` (shared from citizen repo) |
| **G6** | **Ward officers (369)** | Ward-level assignee keys + roster — **not** in citizen app | Official ward officer directory |
| **G7** | **Prioritization dashboard** | Filter by AI severity + citizen severity + repeat reports | Existing AI fields on report |
| **G8** | **SLA tracking** | Time-to-assign, time-to-fix, overdue alerts | Status timestamps in DB |

### Already on citizen side (gov can consume — no citizen work needed)

- Report push to `reports` + `evidence` bucket
- GBA outer boundary + submit validation
- 5 corporation + 369 ward routing snapshot at submit (`0011` columns)
- Confirmation email with corporation + ward (signed-in only)

### Recommended Government App Features (baseline — from original handover)

- Official status update controls:
  - Open -> In Progress -> Completed
- Field-verification capture:
  - Engineer notes, before/after repair images, repair date
- Prioritization dashboard:
  - Filter by AI severity suggestion + citizen severity + repeat reports
- SLA tracking:
  - Time-to-assign, time-to-fix, overdue alerts

## Data Contract Checklist (Citizen -> Government)

- Report identity: `id`, `createdAtMs`, `cityKey`
- Geolocation: `latitude`, `longitude`, `areaLabel`
- Visual evidence: `photoPath` (close-up), `widePhotoPath` (context)
- Severity + narrative: `severity`, `note`
- Lifecycle status: `status`
- Assignment metadata:
  - `assigneeKey`, `assigneeCorporation`, `assigneeZone`, `assigneeName`, `assigneePosition`, `assigneeOfficeAddress`
- **GBA ward routing (v1.0.3+):** `wardKey`, `wardNumber`, `wardName` — migration `0011_report_ward_routing.sql`
- Reporter privacy-safe key: `reporterUserId` (no raw PII)

## GPS Calibration Update (Latest)

- Location capture now uses calibrated selection:
  - Combines last known GPS/network/passive fixes
  - Requests fresh single updates from GPS and network providers
  - Chooses best fix by accuracy + recency score

This reduces the “slightly off from current location” issue seen with stale last-known fixes.

