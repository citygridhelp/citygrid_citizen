# Play Store release notes — v1.0.5

**App:** City Grid (`in.citygrid.citizen`)  
**Track:** Closed testing → production when ready  
**Previous store build:** v1.0.4 (`versionCode` 5) — map area labels (G-lite)  
**This build:** v1.0.5 (`versionCode` 6)

Version is set in `app/build.gradle.kts` (`versionCode = 6`, `versionName = "1.0.5"`).

Related: [release_and_versioning_guide.md](release_and_versioning_guide.md) · [play_store_listing.md](play_store_listing.md) · [future_features_backlog.md](future_features_backlog.md) §17.1

---

## Play Console — release identity (copy/paste)

| Field | Value |
|-------|--------|
| **Release name** (internal label only; testers do not see this) | `1.0.5 — Area heat map` |
| **Version name** (from AAB) | `1.0.5` |
| **Version code** (from AAB) | `6` |
| **Package** | `in.citygrid.citizen` |

---

## What's New — paste into Play Console (user-facing)

**Recommended (concise):**

```text
What's new in City Grid 1.0.5:

• New Heat toggle on the Bengaluru map — see report density by area
• Heat uses the same Severity filter as the map clusters
• Cluster counts stay clear (white disks while Heat is on)
• Heat stays on while you zoom, pan, or use Locate
• Faster return when switching back to the app (fixes stuck white screen)
• Bell notification when a newer version is available on the Play Store
```

**One-line variant (~200 characters):**

```text
Bengaluru Heat map toggle, fix for white screen when returning to the app, and a bell notice when a Play Store update is available.
```

---

## Enhancements included in this release (since v1.0.4)

| # | Area | User-visible change |
|---|------|---------------------|
| **17.1** | Heat map | Capsule **Heat** toggle (top-right, in line with Severity) — Bengaluru only; default off |
| **17.1** | Density | GBA ward areas filled by report count under current Severity filter |
| **17.1** | Clusters | Heat on → white disks + dark numbers; Heat off → red disks as before |
| **17.1** | Borders | Soft/light ward boundary lines (not strong red) |
| **17.1** | Persistence | Heat stays on through zoom / pan / Locate until toggled off (or leave Bengaluru) |
| — | Resume | Fix stuck white/splash screen when returning to the app after background |
| — | Updates | Bell notification when Play has a newer version; tap opens the store |
| — | Weather | City alias lookup hardened (e.g. Bangalore → Bengaluru); weather line unchanged on home |

**Not in this release (next / later):**

| Item | Notes |
|------|--------|
| **#14** Google-like street vs landmark label tiers | Next enhancement after this upload |
| **#17.2** Tap heat area → name / count popup | Later |
| Heat **legend** (Fewer → More) | Later |
| **#19** Tap cluster → view reports | Last among map items |
| **#10.6** Corp reference UI | Optional |

**Backend:** No new migration for this build.

---

## Pre-upload checklist

- [x] `versionCode` → **6**, `versionName` → **1.0.5**
- [x] Heat map (#17.1) in app
- [ ] Android Studio: **Build → Generate Signed Bundle** (or `./gradlew bundleRelease`)
- [ ] Upload AAB to Closed testing (same track as 1.0.4)
- [ ] **Release name:** `1.0.5 — Area heat map`
- [ ] Paste **What's New** from this doc
- [ ] Tell testers to tap **Update** in Play Store
- [ ] Smoke test:
  - [ ] Bengaluru: Heat on → area colors; clusters white with dark numbers
  - [ ] Heat off → red clusters
  - [ ] Zoom / Locate with Heat on → heat stays on
  - [ ] Severity still filters clusters + heat
  - [ ] Other cities: no Heat toggle
  - [ ] Weather line under city title still loads
  - [ ] Leave app → return without force-stop → home appears (no stuck white screen)
  - [ ] After a newer Play build exists: bell shows “Update available”; tap opens store

---

## In-app update notification body

When users upgrade from 1.0.4 → 1.0.5:

```text
City Grid was updated to v1.0.5. New: Heat map on Bengaluru, smoother return to the app, and a notice when a Play Store update is available.
```

---

## Tester reminder (optional message)

> Update City Grid from the Play Store (same closed-test link). You should see **1.0.5**. On Bengaluru, use the **Heat** toggle next to Severity. Area tap names and Google-style street/landmark label tiers are not in this build yet.

---

*Document created for the 1.0.5 area heat map batch after published 1.0.4 (July 2026).*
