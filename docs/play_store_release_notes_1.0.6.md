# Play Store release notes — v1.0.6

**App:** City Grid (`in.citygrid.citizen`)  
**Track:** Closed testing → production when ready  
**Previous store build:** v1.0.5 (`versionCode` 6) — area heat map  
**This build:** v1.0.6 (`versionCode` 7)

Version is set in `app/build.gradle.kts` (`versionCode = 7`, `versionName = "1.0.6"`).

Related: [release_and_versioning_guide.md](release_and_versioning_guide.md) · [play_store_listing.md](play_store_listing.md) · [future_features_backlog.md](future_features_backlog.md) §14

---

## Play Console — release identity (copy/paste)

| Field | Value |
|-------|--------|
| **Release name** (internal label only; testers do not see this) | `1.0.6 — Map place & street names` |
| **Version name** (from AAB) | `1.0.6` |
| **Version code** (from AAB) | `7` |
| **Package** | `in.citygrid.citizen` |

---

## What's New — paste into Play Console (user-facing)

**Recommended (concise):**

```text
What's new in City Grid 1.0.6:

• Place, street, and water names on the map from the basemap (zoom in to see more detail)
• Slightly clearer map labels for easier reading
• Same labeled map on report details
• Heat map, clusters, and severity filters work as before
```

**One-line variant (~200 characters):**

```text
Map place and street names from the basemap, clearer labels, and the same Heat / severity tools as before.
```

---

## Enhancements included in this release (since v1.0.5)

| # | Area | User-visible change |
|---|------|---------------------|
| **14** | Map labels | Home map uses CARTO **light_all** tiles — place / street / water names follow zoom & pan |
| **14** | Map labels | Removed curated lat/lon overlay (seeded names); names come from the basemap |
| **14** | Readability | Mild darker treatment on map tiles so names read better against heat / fills |
| **14** | Report detail | Report location map uses the same labeled basemap + contrast tweak |

**Unchanged from 1.0.5 (still in the app):** Heat toggle, severity filter, clusters, resume fix, Play update bell.

**Not in this release (next / later):**

| Item | Notes |
|------|--------|
| Darker map labels (stronger filter) | **Remind later** — current mild darken looks fine; revisit if testers ask |
| **#17.2** Tap heat area → name / count popup | Later |
| Heat **legend** (Fewer → More) | Later |
| **#19** Tap cluster → view reports | Last among map items |
| **#20** Weekly digest | Needs product design |

**Backend:** No new migration for this build.

---

## Pre-upload checklist

- [x] `versionCode` → **7**, `versionName` → **1.0.6**
- [x] Basemap labels (#14) in app
- [ ] Android Studio: **Build → Generate Signed Bundle** (or `./gradlew bundleRelease`)
- [ ] Upload AAB to Closed testing (same track as 1.0.5)
- [ ] **Release name:** `1.0.6 — Map place & street names`
- [ ] Paste **What's New** from this doc
- [ ] Tell testers to tap **Update** in Play Store
- [ ] Smoke test:
  - [ ] Bengaluru: street / place / water names visible when zoomed in
  - [ ] Heat on → labels still readable; clusters still on top
  - [ ] Heat off → red clusters as before
  - [ ] Severity still filters clusters + heat
  - [ ] Report detail map shows labeled basemap
  - [ ] Other cities: labeled map (no Heat toggle)

---

## In-app update notification body

When users upgrade from 1.0.5 → 1.0.6:

```text
City Grid was updated to v1.0.6. New: place and street names on the map from the basemap, with clearer label contrast.
```

---

## Tester reminder (optional message)

> Update City Grid from the Play Store (same closed-test link). You should see **1.0.6**. Zoom the Bengaluru map for place and street names. Heat and Severity work as in 1.0.5. Tell us if labels need to be darker.

---

*Document created for the 1.0.6 basemap-labels batch after published 1.0.5 (July 2026).*
