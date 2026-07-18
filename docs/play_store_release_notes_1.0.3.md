# Play Store release notes — v1.0.3

**App:** City Grid (`in.citygrid.citizen`)  
**Track:** Closed testing → production when ready  
**Previous store build:** v1.0.2 (`versionCode` 3)  
**This build:** v1.0.3 (`versionCode` 4)

Version is set in `app/build.gradle.kts` (`versionCode = 4`, `versionName = "1.0.3"`).

Related: [release_and_versioning_guide.md](release_and_versioning_guide.md) · [play_store_listing.md](play_store_listing.md)

---

## Play Console — release identity (copy/paste)

| Field | Value |
|-------|--------|
| **Release name** (internal label only; testers do not see this) | `1.0.3 — GBA ward routing, GPS accuracy` |
| **Version name** (from AAB) | `1.0.3` |
| **Version code** (from AAB) | `4` |
| **Package** | `in.citygrid.citizen` |

---

## What's New — paste into Play Console (user-facing)

**Recommended (concise):**

```text
What's new in City Grid 1.0.3:

• Bengaluru reports now show your GBA corporation and ward after submit
• Ward and corporation appear in report details and Accountability
• GPS accuracy on the report screen — warns on weak signal, blocks very poor fixes
• Signed-in confirmation emails include corporation and ward (when enabled)
```

**One-line variant (~200 characters):**

```text
GBA corporation and ward routing when you submit, shown in report details; GPS accuracy warnings on the report screen; confirmation email improvements.
```

---

## Enhancements included in this release

| # | Area | User-visible change | Key files |
|---|------|---------------------|-----------|
| 10.5 | Routing | GPS → official 369 GBA wards → corporation + ward snapshot on new Bengaluru reports | `BengaluruGbaWards.kt`, `BengaluruMunicipalRouting.kt`, `bengaluru_gba_wards.json` |
| 10.1 / 10.4 | Accountability | Five GBA corporations (not legacy BBMP zones) on new reports; Ward line in report details | `MunicipalContactsRegistry.kt`, `ReportDetailDialog.kt`, `AccountabilitySection.kt` |
| — | Submit UX | Snackbar shows routed corporation + ward after save | `NewReportScreen.kt`, `BengaluruMunicipalRouting.kt` |
| 12 | GPS | Report screen shows `GPS ±Xm`; warn >50 m; block auto-GPS submit >100 m; manual coords bypass | `NewReportScreen.kt` |
| 8 (extend) | Email | Confirmation email adds Corporation + Ward lines (edge function redeploy) | `notify-citizen-report-created/index.ts` |

**Backend (before testers sync signed-in reports):**

| Item | Notes |
|------|--------|
| Migration `0011_report_ward_routing.sql` | Adds `ward_key`, `ward_number`, `ward_name` on `reports` — **already applied** if you ran it before upload |
| Edge function redeploy | Optional — only if confirmation emails should include ward/corp |

**Not in this release (next citizen backlog):**

| Item | Notes |
|------|--------|
| #14 Map area labels | More neighborhood names on zoom-in |
| #10.6 Corp reference screen | Optional read-only directory |
| #17 GBA heat map | Major — division density overlay |
| #19 Tap cluster → reports | Testing polish |
| Gov app / seed officers | Parked — [government_app_handover.md](government_app_handover.md) |

---

## Pre-upload checklist

- [x] `versionCode` → **4**, `versionName` → **1.0.3**
- [ ] `./gradlew bundleRelease` (or Android Studio **Build → Generate Signed Bundle**)
- [ ] Upload AAB to Closed testing (same closed-test link as 1.0.2)
- [ ] **Release name:** `1.0.3 — GBA ward routing, GPS accuracy`
- [ ] Paste **What's New** from this doc
- [ ] Tell testers to tap **Update** in Play Store (may not auto-update immediately)
- [ ] Smoke test: submit in Bengaluru → snackbar shows corp + ward; report details → Ward line; GPS warn/block on report screen
- [ ] Signed-in: Supabase row has `ward_*` columns filled

---

## In-app update notification body

When users upgrade from 1.0.2 → 1.0.3, `CitizenNotificationsRepository.checkAppVersion` adds an **App updated** notice. Suggested body:

```text
City Grid was updated to v1.0.3. New: GBA corporation and ward shown when you submit a report, plus GPS accuracy checks on the report screen.
```

---

*Document created for the 1.0.3 enhancement batch (July 2026).*
