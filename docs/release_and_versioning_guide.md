# City Grid — Release, Versioning & Update Guide

Step-by-step process for maintaining, testing, versioning, releasing, and notifying users across:

| Component | Package / location | Role |
|-----------|-------------------|------|
| **Citizen app** | `com.example.potholereport` — this repo (`potholereport/`) | Report potholes, My Reports, notifications |
| **Government app** | `pothole.govt` — separate Android Studio project | Officer workflow, status updates, completion proofs |
| **Shared backend** | Supabase — `supabase/migrations/` in this repo | Auth, `reports` table, storage, RPCs |

Both apps talk to the **same Supabase project** in production. Schema changes must stay backward-compatible until all live app versions are updated.

---

## Table of contents

1. [Architecture & release order](#1-architecture--release-order)
2. [Environments & secrets](#2-environments--secrets)
3. [Version numbering](#3-version-numbering)
4. [Day-to-day maintenance workflow](#4-day-to-day-maintenance-workflow)
5. [Testing before every release](#5-testing-before-every-release)
6. [Backend (Supabase) changes](#6-backend-supabase-changes)
7. [Citizen app release](#7-citizen-app-release)
8. [Government app release](#8-government-app-release)
9. [Update notifications (citizen app)](#9-update-notifications-citizen-app)
10. [Play Store upload & staged rollout](#10-play-store-upload--staged-rollout)
11. [Rollback without breaking live users](#11-rollback-without-breaking-live-users)
12. [Release checklist (printable)](#12-release-checklist-printable)
13. [File reference — where to change what](#13-file-reference--where-to-change-what)
14. [Recommended next improvements](#14-recommended-next-improvements)

---

## 1. Architecture & release order

```text
┌─────────────────┐     insert reports      ┌──────────────┐     status / proofs     ┌─────────────────┐
│  Citizen app    │ ──────────────────────► │   Supabase   │ ◄────────────────────── │ Government app  │
│  potholereport  │ ◄── sync My Reports ─── │   (Postgres  │ ── officer workflow ──► │  pothole.govt   │
└─────────────────┘                         │   + Storage) │                         └─────────────────┘
                                            └──────────────┘
```

**Golden rule for every release that touches backend + apps:**

```text
1. Apply backward-compatible Supabase migration (staging → production)
2. Verify migration on staging with OLD app builds still working
3. Release government app (if it uses new RPC/columns) OR citizen app — whichever consumes the change first
4. Release the other app
5. Monitor crashes / sync errors for 24–48 hours
6. Increase Play Store rollout from 10% → 100%
```

Never ship an app that **requires** a DB column before the migration is live. Never run a **breaking** migration while old app versions are still in the field.

---

## 2. Environments & secrets

### 2.1 Local credentials (never commit)

**Citizen app** — project root `local.properties` (git-ignored):

```properties
sdk.dir=C\:\\Users\\YOU\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://YOUR-PROJECT.supabase.co
SUPABASE_ANON_KEY=eyJ...anon...key
```

Read by: `app/build.gradle.kts` → `BuildConfig.SUPABASE_URL` / `SUPABASE_ANON_KEY`  
Used by: `app/src/main/java/.../data/remote/SupabaseClientProvider.kt`

**Government app** — same pattern in its own project’s `local.properties` (anon key only, never service_role in apps).

### 2.2 Staging vs production (recommended)

| | Staging | Production |
|---|---------|------------|
| Supabase | Separate project or branch | Live project |
| `local.properties` | Staging URL + anon key | Prod URL + anon key |
| Play Console | Internal / closed track | Production track |
| Officer seed | `supabase/seed/seed_officers.mjs` on staging | Run once on prod |

Test every migration and both apps against **staging** before touching production.

### 2.3 Service role key

Used **only** on your PC for admin scripts (e.g. `supabase/seed/seed_officers.mjs`).  
Never embed in either Android app.

---

## 3. Version numbering

### 3.1 Where to change (citizen app)

**File:** `app/build.gradle.kts`

```kotlin
defaultConfig {
    applicationId = "com.example.potholereport"
    versionCode = 2        // ← MUST increase every Play upload (integer)
    versionName = "1.1.0"  // ← User-visible string (semver)
}
```

**Government app:** same fields in its `app/build.gradle.kts` with `applicationId = "pothole.govt"` (or your actual id).

### 3.2 Semver convention

| Bump | Example | When |
|------|---------|------|
| **PATCH** | `1.0.0` → `1.0.1` | Bug fixes, UI, no schema change |
| **MINOR** | `1.0.1` → `1.1.0` | New features, backward-compatible backend |
| **MAJOR** | `1.9.0` → `2.0.0` | Breaking API/schema; coordinate forced upgrade |

**Rules:**

- `versionCode` always **+1** per store upload (even rollback releases).
- `versionName` should match git tag: `v1.1.0`.
- Citizen and government apps **version independently** but document compatible backend migration numbers in release notes.

### 3.3 Git tagging

After a successful production release:

```powershell
git tag -a v1.1.0 -m "Citizen app 1.1.0 — map fixes, My Reports dark theme"
git push origin v1.1.0
```

Keep one tag per released citizen version. Tag the government repo separately if it is another git project.

---

## 4. Day-to-day maintenance workflow

### 4.1 Citizen app (this repo)

```text
1. Pull latest main
2. Create branch: feature/my-change or fix/issue-name
3. Edit code under app/src/main/java/com/example/potholereport/
4. Run debug on device: Android Studio ▶ Run (installDebug)
5. ./gradlew assembleDebug   (CI-style compile check)
6. Commit → PR → merge to main
7. When ready to ship: follow §7 + §10
```

### 4.2 Shared backend

```text
1. Add new file: supabase/migrations/0008_description.sql
2. Test on staging Supabase (SQL Editor or supabase db push)
3. Update supabase/README.md if officers/seed steps change
4. Merge migration to main before app release that depends on it
```

### 4.3 Government app

Same branch/PR flow in the **government Android project**. When changing shared RPCs or columns, update:

- `docs/government_app_handover.md` (capability checklist)
- Field mapping in `supabase/README.md`

### 4.4 Assets (launcher / splash)

| Asset | Path | Regenerate |
|-------|------|------------|
| Short launcher master | `app/src/main/res/drawable-nodpi/city_grid_icon_short.png` | `powershell -File tools/regenerate_launcher_icons.ps1` |
| Splash master | `app/src/main/res/drawable-nodpi/city_grid_splash.png` | Replace PNG; rebuild app |
| Manifest icon | `@mipmap/new_app_icon_launcher` | `app/src/main/AndroidManifest.xml` |

---

## 5. Testing before every release

### 5.1 Automated compile

```powershell
cd C:\Users\priye\AndroidStudioProjects\potholereport
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew assembleDebug assembleRelease
```

Both tasks must succeed. Repeat in the government project.

### 5.2 Citizen app — manual smoke test

Run on **至少 one physical phone** in light **and** dark theme:

| # | Test | Pass? |
|---|------|-------|
| 1 | Cold start → splash → home loads | |
| 2 | Map pan/zoom, city picker, GPS | |
| 3 | Submit report **signed in** → appears in My Reports | |
| 4 | Report syncs to Supabase (check dashboard or gov app) | |
| 5 | Submit report **signed out** → local only (expected today) | |
| 6 | Notifications bell → list opens, mark read | |
| 7 | Sign out / sign in → session restore | |
| 8 | Airplane mode submit → saved locally; note sync message | |
| 9 | Upgrade from **previous release** install (not clean install) | |

### 5.3 Government app — manual smoke test

| # | Test | Pass? |
|---|------|-------|
| 1 | Officer login | |
| 2 | See citizen-submitted report in queue | |
| 3 | Change status Open → In Progress → Completed | |
| 4 | Upload completion proof (if applicable) | |
| 5 | Citizen My Reports shows updated status after refresh | |

### 5.4 Backend regression

After new migration:

- Old citizen APK (previous versionCode) can still open and sync.
- New citizen APK can insert/read reports.
- Government app workflow RPC still succeeds.

---

## 6. Backend (Supabase) changes

### 6.1 Migration files (in order)

```text
supabase/migrations/
  0001_init.sql
  0002_email_exists.sql
  0003_officer_workflow_sync.sql
  0004_status_history_rpc.sql
  0005_officer_completion.sql
  0006_protect_citizen_completion.sql
  0007_citizen_profiles.sql
  0008_your_change.sql          ← add new files here
```

See `supabase/README.md` for apply instructions.

### 6.2 How to apply

**Option A — Dashboard:** Supabase → SQL Editor → paste new migration → Run.

**Option B — CLI:** `supabase db push` (if CLI linked to project).

**Order:** staging first → smoke test → production.

### 6.3 Safe migration patterns

| Safe | Unsafe |
|------|--------|
| `ADD COLUMN ... DEFAULT NULL` | `DROP COLUMN` while old apps read it |
| New RPC; old apps ignore it | Rename column without dual-write period |
| New optional Storage path | Change enum values without app update |

### 6.4 Government officers — roster, seeding, and passwords

Officer logins for **CG GOVT** live in Supabase Auth + `public.gov_officers`. The source of truth for the roster file is:

| File | Purpose |
|------|---------|
| `supabase/seed/officers.json` | Officer emails, roles, city, zone, `assigneeKey` |
| `supabase/seed/seed_officers.mjs` | Admin script: creates Auth users + upserts `gov_officers` |

**Also keep in sync when zones or names change (separate Android project):**

| File (Potholegovt) | Purpose |
|----------------------|---------|
| `app/.../data/MunicipalOfficersRegistry.kt` | GPS zone routing (nearest zone head) |
| `app/.../data/GovAuthRepository.kt` | Local demo logins when Supabase is not configured |
| `app/.../data/MunicipalContactsRegistry.kt` (citizen repo) | Citizen accountability routing — should match BBMP zones |

#### When to run `seed_officers.mjs`

| Situation | Run seed? |
|-----------|-----------|
| First-time Supabase setup for government logins | **Yes** |
| Added/changed an officer in `officers.json` | **Yes** |
| Need to reset all officer passwords to one value | **Yes** (see below) |
| Only changed app UI / routing code | **No** — officers refresh tickets with **Refresh** in the app |
| Supabase not configured (demo-only local auth) | **No** |

Re-running is **safe and idempotent**: existing Auth users are found by email, `gov_officers` is upserted, and password is reset to `OFFICER_PASSWORD`.

#### How to add or update an official

1. Edit `supabase/seed/officers.json`. Each entry needs:
   - `email` — login for CG GOVT (lowercase)
   - `displayName`, `position`, `corporation`, `zoneLabel`
   - `role` — `COMMISSIONER`, `ZONE_HEAD`, or `FIELD_OFFICER`
   - `cityKey` — e.g. `BENGALURU`, `MUMBAI`
   - `assigneeKey` — zone head key, e.g. `BENGALURU:BOMMANAHALLI` (empty for commissioners)
   - `fieldOfficerKey` — e.g. `BENGALURU:EAST:ENG_01` (field officers only)

2. Update zone routing in the **government app** (`MunicipalOfficersRegistry.kt`) and **citizen app** (`MunicipalContactsRegistry.kt`) if the zone is new or coordinates changed.

3. Optionally mirror the account in `GovAuthRepository.kt` for offline demo builds (`ENABLE_DEMO_DATA`).

4. Run the seed script (staging first, then production):

```powershell
cd supabase/seed
npm install
$env:SUPABASE_URL="https://YOUR-PROJECT.supabase.co"
$env:SUPABASE_SERVICE_ROLE_KEY="<service_role>"   # Dashboard → Settings → API — NEVER put in Android apps
$env:OFFICER_PASSWORD="your-secure-password"      # optional; defaults to gov123
node seed_officers.mjs
```

5. Sign in on a device with the new email to verify role and dashboard (commissioner vs zone head vs field officer).

**Role reminder (BBMP example):**

| Role in app | Who | Typical login |
|-------------|-----|----------------|
| `COMMISSIONER` | City-wide Municipal Commissioner / admin — sees all city tickets, can reassign | `commissioner.bengaluru@bbmp.gov.in` |
| `ZONE_HEAD` | Joint Commissioner of one zone — owns tickets routed to that zone | e.g. `ajith.bommanahalli@bbmp.gov.in` → `BENGALURU:BOMMANAHALLI` |
| `FIELD_OFFICER` | Engineer under a zone head — delegated work only | e.g. `ramesh.east@bbmp.gov.in` |

A **Joint Commissioner** (zone head) is not the same as the **Municipal Commissioner** (city role), even though both titles contain “Commissioner”.

#### How to change officer passwords

**Option A — Reset all seeded officers (recommended for bulk reset)**

Set `OFFICER_PASSWORD` and re-run `seed_officers.mjs` (same commands as above). Every officer in `officers.json` gets that password.

**Option B — Change one officer in Supabase Dashboard**

1. Supabase → **Authentication** → **Users**
2. Find the officer email → **Send password recovery** or update via admin API
3. No change to `officers.json` required

**Option C — Per-officer via script**

Run `seed_officers.mjs` after editing only that officer’s row in `officers.json`, or extend the script locally for one-off passwords (not in repo today).

**Security notes:**

- Use **`SUPABASE_SERVICE_ROLE_KEY`** only on a trusted PC or CI — never in `local.properties` or either Android app.
- Change default `gov123` before any production handover.
- After password change, officers must sign in again on each device.

#### Ticket assignment vs officer roster

- Seeding creates **logins**; it does not rewrite existing pothole rows in `reports`.
- After zone-routing fixes, officers should tap **Refresh** on the dashboard to sync tickets and assignees from Supabase.
- New citizen reports get assignee metadata from GPS routing (`assignee_key` in `reports`); commissioners should not be the default zone owner.

See also: [government_app_handover.md](government_app_handover.md) § Government officers & Supabase.

---

## 7. Citizen app release

### Step 1 — Bump version

Edit `app/build.gradle.kts`:

```kotlin
versionCode = 3        // was 2
versionName = "1.1.0"
```

### Step 2 — Update changelog (recommended)

Add a short entry in git tag message or `CHANGELOG.md` if you create one:

```text
1.1.0 — My Reports dark theme, tab label scaling, launcher icon, MapView crash fix
```

### Step 3 — Build signed release bundle

**First time only:** Android Studio → Build → Generate Signed Bundle / APK → create keystore → store password in password manager (never commit `.jks`).

**Each release:**

```powershell
.\gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Or: Android Studio → Build → Generate Signed Bundle → **Android App Bundle**.

### Step 4 — Verify bundle

- Install release APK on device if you also build `assembleRelease` for sideload testing.
- Confirm `versionName` in About/footer matches.

### Step 5 — Upload (see §10)

### Step 6 — Post-release notification behavior

After users install the new APK, the app automatically creates an in-app notification (see §9). No extra step unless you add remote config later.

---

## 8. Government app release

The government app lives in a **separate project** (`pothole.govt`). Process mirrors the citizen app:

| Step | Where |
|------|--------|
| Bump `versionCode` / `versionName` | Government `app/build.gradle.kts` |
| Point `local.properties` at same prod Supabase | Same URL/anon as citizen (prod) |
| Run smoke tests §5.3 | Physical device |
| `./gradlew bundleRelease` | Government project root |
| Upload AAB | Separate Play Console listing (or internal MDM) |

**Coordination:**

- If release uses new RPC from `0003`–`0007`, deploy migration **before** government APK.
- If release only changes UI, can ship independently of citizen app.
- Document which backend migration version each gov app build requires in release notes.

**Officers & passwords:** When adding zone heads or rotating credentials, update `officers.json` and run `seed_officers.mjs` (§6.4) on staging then production before telling staff to use new logins.

---

## 9. Update notifications (citizen app)

### 9.1 What exists today (automatic)

When the app starts or refreshes, `AppAutoRefresh` calls:

```text
CitizenNotificationsRepository.checkAppVersion(BuildConfig.VERSION_NAME)
```

**Files involved:**

| File | Role |
|------|------|
| `app/.../data/AppAutoRefresh.kt` | Calls check on splash / resume |
| `app/.../data/CitizenNotificationsRepository.kt` | Compares to `last_seen_app_version` in SharedPreferences |
| `app/.../data/CitizenNotification.kt` | Type `APP_UPDATE` |
| `app/.../ui/home/NotificationsDialog.kt` | Shows notification list |
| `app/.../ui/home/HomeScreen.kt` | Bell icon + unread count (signed-in users) |

**Behavior:**

1. User had `versionName` **1.0** stored locally.
2. User installs **1.1** from Play Store.
3. On next open, app detects change → adds notification:

   - Title: **App updated**
   - Body: **City Grid was updated to v1.1.0…**
   - Type: `APP_UPDATE` (blue icon in notifications dialog)

4. User opens account menu → Notifications → sees the entry.

**Limitations today:**

- Fires **after** update only (local version compare).
- Unread badge on bell is for **signed-in** users only.
- Does not push “please update” to users still on old store version.

### 9.2 What you should write in release notes (for the notification body)

The body is **hardcoded** in `CitizenNotificationsRepository.checkAppVersion`. For important releases, edit the template when bumping version:

**File:** `app/src/main/java/com/example/potholereport/data/CitizenNotificationsRepository.kt`

```kotlin
body = "City Grid was updated to v$version. " +
    "What's new: improved map performance, My Reports dark theme, and sync fixes.",
```

Ship that text in the **same release** as the version bump so the notification describes the right changes.

### 9.3 Optional upgrade path (future) — remote update config

For “update available” **before** users upgrade, add a Supabase table:

```sql
-- Example future migration
CREATE TABLE app_config (
  key text PRIMARY KEY,
  value text NOT NULL
);
INSERT INTO app_config VALUES
  ('citizen_min_version_code', '2'),
  ('citizen_latest_version_name', '1.1.0'),
  ('citizen_update_message', 'Please update for report sync improvements.');
```

App reads on splash → if `VERSION_CODE < min` show blocking dialog; if `< latest` add `GENERAL` notification. Document this in a new migration when implemented.

### 9.4 Government app notifications

If the government app needs similar behavior, replicate the pattern:

- `GovNotificationsRepository.checkAppVersion(BuildConfig.VERSION_NAME)`
- Call from that app’s splash/main refresh
- Separate `last_seen_app_version` prefs key (different package name = separate storage automatically)

---

## 10. Play Store upload & staged rollout

### 10.1 First-time setup

1. [Google Play Console](https://play.google.com/console) → Create app **City Grid** (citizen).
2. Create second app entry for government (or distribute via enterprise MDM).
3. Complete store listing, content rating, privacy policy.
4. Upload signing key (Play App Signing recommended).

### 10.2 Every citizen release

```text
1. Play Console → City Grid → Release → Production (or Testing)
2. Create new release → Upload app-release.aab
3. Release name: "1.1.0 (3)" matching versionName (versionCode)
4. Paste release notes (user-visible):
     • Improved map performance
     • My Reports readable in dark mode
     • Bug fixes
5. Save → Review → Start rollout to Production
6. Set staged rollout: 10% → monitor 24h → 50% → 100%
```

### 10.3 Internal testing track (recommended gate)

```text
Upload AAB to Internal testing → share opt-in link → testers install →
Run §5.2 checklist → promote same AAB to Production
```

### 10.4 Monitor after upload

- Play Console → **Android vitals** (crashes, ANRs).
- Supabase → logs / report insert rate.
- User feedback on sync (“Report saved on device. Cloud upload failed…”).

---

## 11. Rollback without breaking live users

### 11.1 Android app rollback

Play Store **cannot** install a lower `versionCode`.

| Problem | Action |
|---------|--------|
| Bad release rolling out | Play Console → **Halt rollout** immediately |
| Fix ready | Revert bad commit in git → bump **new** `versionCode` (e.g. 4) → ship fixed AAB |
| Users already on bad build | They must update to the new fix release |

Keep previous good AAB and keystore access. Tag good releases in git.

### 11.2 Supabase rollback

Do **not** delete old migration files. Add a **new** migration that reverses the change:

```sql
-- 0009_revert_0008_example.sql
ALTER TABLE reports ADD COLUMN IF NOT EXISTS old_field text;
-- or restore RPC body from previous version
```

Restore from Supabase **backup** only for catastrophic data loss (Dashboard → Database → Backups).

### 11.3 Data on user phones

Citizen reports persist in:

- `SharedPreferences` JSON (`pothole_recent_reports`)
- `filesDir/report_photos/`

New app versions must keep reading old JSON (`optString`, defaults). Never wipe this data on upgrade.

### 11.4 Coordinated rollback example

```text
1. Halt citizen app rollout (versionCode 3 bad)
2. If migration 0008 broke old apps: deploy 0009 fix migration
3. Ship citizen versionCode 4 with code fix OR reverted code from tag v1.0.0
4. Verify gov app still syncs
5. Resume rollout at 10%
```

---

## 12. Release checklist (printable)

### Pre-release

```text
[ ] Feature branch merged to main
[ ] Supabase migration tested on staging (if any)
[ ] Migration applied to production BEFORE app needs it
[ ] versionCode incremented (citizen)
[ ] versionName updated (citizen)
[ ] Notification body updated in CitizenNotificationsRepository (if user-facing changes)
[ ] versionCode / versionName incremented (government, if shipping)
[ ] ./gradlew assembleRelease — success (both apps)
[ ] Manual smoke test §5.2 (citizen)
[ ] Manual smoke test §5.3 (government, if changed)
[ ] Signed app-release.aab built
[ ] Git tag vX.Y.Z created
```

### Upload

```text
[ ] AAB uploaded to Internal testing → verified
[ ] Release notes written (Play Store + in-app notification text)
[ ] Production rollout started at 10%
[ ] Android vitals checked after 24h
[ ] Rollout increased to 100%
```

### Post-release

```text
[ ] Confirm APP_UPDATE notification appears after test upgrade
[ ] Confirm new reports appear in government queue
[ ] Confirm status round-trip to citizen My Reports
[ ] Archive AAB + mapping file in secure storage
```

---

## 13. File reference — where to change what

### Citizen app

| Goal | File(s) |
|------|---------|
| Version numbers | `app/build.gradle.kts` |
| Supabase URL/keys | `local.properties` (not in git) |
| Report upload to cloud | `app/.../data/remote/ReportSyncRepository.kt` |
| Local report storage | `app/.../data/RecentReportsRepository.kt` |
| Splash / auto refresh | `app/.../data/AppAutoRefresh.kt` |
| Update notification text | `app/.../data/CitizenNotificationsRepository.kt` → `checkAppVersion` |
| Notification UI | `app/.../ui/home/NotificationsDialog.kt` |
| App icon | `drawable-nodpi/city_grid_icon_short.png` + `tools/regenerate_launcher_icons.ps1` |
| Launcher manifest | `app/src/main/AndroidManifest.xml` |
| About / footer version string | `HomeScreen.kt` (footer shows v0.1.0 — update when marketing version changes) |

### Backend

| Goal | File(s) |
|------|---------|
| Schema / RPC | `supabase/migrations/000N_*.sql` |
| Apply docs | `supabase/README.md` |
| Officer accounts (roster + seed script) | `supabase/seed/officers.json`, `seed_officers.mjs` — see §6.4 |
| Field mapping citizen ↔ DB ↔ gov | `supabase/README.md` § Field mapping |

### Documentation

| Goal | File(s) |
|------|---------|
| Government feature handover | `docs/government_app_handover.md` |
| This release guide | `docs/release_and_versioning_guide.md` |

### Government app (separate repo)

| Goal | File(s) |
|------|---------|
| Version numbers | `app/build.gradle.kts` |
| Supabase keys | `local.properties` |
| Status / workflow sync | Gov app sync layer calling RPCs from `0003`–`0005` |
| Zone GPS routing (assignee) | `app/.../data/MunicipalOfficersRegistry.kt` |
| Demo officer logins (no Supabase) | `app/.../data/GovAuthRepository.kt` |
| Live officer logins | `supabase/seed/officers.json` + §6.4 seed script |

---

## 14. Recommended next improvements

Priority order for production hardening:

1. **Report upload outbox** — retry failed Supabase pushes after sign-in / network restore (`ReportSyncRepository` + pending flag on `PersistedPotholeReport`).
2. **Staging Supabase project** — never test migrations on production first.
3. **Remote min version** — Supabase `app_config` + splash check (§9.3).
4. **Firebase Crashlytics** or Play vitals alerts — know about crashes before 100% rollout.
5. **CHANGELOG.md** in repo — one line per version; link from release notes.
6. **CI build** — GitHub Action on tag to run `./gradlew bundleRelease` and archive AAB.

---

## Quick command reference (Windows)

```powershell
# Citizen project root
cd C:\Users\priye\AndroidStudioProjects\potholereport
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Debug install on connected phone
.\gradlew installDebug

# Release build
.\gradlew assembleRelease
.\gradlew bundleRelease

# Regenerate launcher icons
powershell -File tools\regenerate_launcher_icons.ps1

# Seed / update government officers
cd supabase\seed
npm install
$env:SUPABASE_URL="https://YOUR-PROJECT.supabase.co"
$env:SUPABASE_SERVICE_ROLE_KEY="<service_role>"
$env:OFFICER_PASSWORD="your-secure-password"
node seed_officers.mjs
cd ..\..

# Git tag after release
git tag -a v1.1.0 -m "Citizen 1.1.0"
git push origin v1.1.0
```

---

*Last updated for repo state: citizen app `versionCode 1` / `versionName 1.0`, Supabase migrations through `0007_citizen_profiles.sql`.*
