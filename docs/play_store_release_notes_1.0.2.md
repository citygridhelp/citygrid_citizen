# Play Store release notes — v1.0.2

**App:** City Grid (`in.citygrid.citizen`)  
**Track:** Closed testing → production when ready  
**Previous store build:** v1.0.1 (`versionCode` 2)  
**This build:** v1.0.2 (`versionCode` 3)

Version is set in `app/build.gradle.kts` (`versionCode = 3`, `versionName = "1.0.2"`).

Related: [release_and_versioning_guide.md](release_and_versioning_guide.md) · [play_store_listing.md](play_store_listing.md)

---

## Play Console — release identity (copy/paste)

| Field | Value |
|-------|--------|
| **Release name** (internal label only; testers do not see this) | `1.0.2 — GBA map, notifications, severity filter` |
| **Version name** (from AAB) | `1.0.2` |
| **Version code** (from AAB) | `3` |
| **Package** | `in.citygrid.citizen` |

---

## What's New — paste into Play Console (user-facing)

**Recommended (concise):**

```text
What's new in City Grid 1.0.2:

• Notifications bell for app updates and report status (signed in)
• Map severity filter and smarter report markers
• Official GBA map boundary for Bengaluru — pan stays inside the city
• Location check blocks submit outside the city; clearer save progress
• Signup OTP resume, improved map controls, UI polish
```

**One-line variant (~200 characters):**

```text
Notifications, GBA map boundary, severity filter, smarter clusters, location checks on submit, signup fixes, and map layout improvements.
```

---

## Enhancements included in this release

| # | Area | User-visible change | Key files |
|---|------|---------------------|-----------|
| 6 | Notifications | Header bell + unread badge (signed-in); account menu → Notifications; status-change notices on sync | `CitizenNotificationsRepository.kt`, `NotificationsDialog.kt`, `HomeScreen.kt`, `RecentReportsRepository.kt` |
| 5 | Submit | Report blocked if GPS / manual coordinates are outside selected city metro | `CityMetroLocation.kt`, `NewReportScreen.kt` |
| — | Map bounds | Pan/zoom clamped to official **GBA** boundary for Bengaluru (Sept 2025); red city outline matches published GIS; submit blocked outside polygon | `BengaluruGbaBoundary.kt`, `HomeScreen.kt`, `CityMetroLocation.kt` |
| — | Map clusters | Individual numbered markers when zoomed in; merge to summed clusters only when circles would overlap on screen | `HomeScreen.kt` (`buildReportScreenOverlapClusters`) |
| — | Map filter | **Severity** dropdown (All / Minor / Moderate / Severe / Critical) replaces “Active critical reports” link | `HomeScreen.kt` (`ReportAndTrackSection`, `attachReportClusterOverlay`) |
| — | Map chrome | **GPS accuracy** overlay top-right on map; zoom FABs stack above **Locate me** bottom-right (same spacing); severity row above map only | `HomeScreen.kt` (`OsmDensityMap`) |
| 13 | Submit UX | Submit button shows SUBMITTING… + spinner; form locked during save/upload | `NewReportScreen.kt` |
| 15 | Map camera | Passive GPS updates move pin only; camera moves on Locate me or city change | `HomeScreen.kt` |
| 16 | Map filter | Severity filter persists across pan/zoom (no auto-clear) | `HomeScreen.kt` |
| 18 | UI | Pill/capsule Report a Pothole CTA + matching New Report submit button | `HomeScreen.kt`, `NewReportScreen.kt` |
| 11 / 11G | Auth | Signup OTP interrupt + resend for unconfirmed email; modal uses separate sign-in vs signup fields | `SupabaseAuthRepository.kt`, `SignupScreen.kt`, `ReportSignInModal.kt` |

**Not in this release (deferred):**

| Item | Notes |
|------|--------|
| Heat map toggle | Planned top row next to Severity (#17); division boundaries depend on #10 |
| #12 GPS accuracy gate | Field testing before warn/block on poor accuracy |
| #14 Map area labels | Backlog |
| #19 Tap cluster → reports | Backlog |

---

## Map control layout (v1.0.2)

| Corner / zone | Control | Always visible? |
|---------------|---------|-----------------|
| Top-left | Compass rose (N/E/S/W) | Yes |
| Top-right | GPS accuracy pill (`GPS ±12m` or `GPS searching…`) | Yes |
| Above map | **Severity** dropdown (All / Minor / … / Critical) | Yes |
| Bottom-right (bottom) | **Locate me** | Yes |
| Bottom-right (above Locate) | Zoom in, Zoom out, City view, Wide zoom out | After map touch (~2.4s auto-hide) |

Gestures: pinch zoom, drag pan (within city bounds).

---

## In-app update notification body

When users upgrade from 1.0.1 → 1.0.2, `CitizenNotificationsRepository.checkAppVersion` adds an **App updated** notice. Suggested body:

```text
City Grid was updated to v1.0.2. New: notifications bell, map severity filter, smarter clusters, and map layout improvements. Check Recent Reports and the home map.
```

---

## Pre-upload checklist

- [x] `versionCode` → **3**, `versionName` → **1.0.2**
- [ ] `./gradlew bundleRelease` (or Android Studio **Build → Generate Signed Bundle**)
- [ ] Upload AAB to Closed testing
- [ ] Paste **What's New** from this doc
- [ ] Upload `mapping.txt` for versionCode 3 ([play_console_release_warnings.md](play_console_release_warnings.md))
- [ ] Smoke test: map severity filter, GPS pill, zoom stack + Locate, notifications bell, submit outside city blocked
- [ ] Mark shipped items in [future_features_backlog.md](future_features_backlog.md) when release is live

---

*Document created for the 1.0.2 enhancement batch (July 2026). Update this file if scope changes before store upload.*
