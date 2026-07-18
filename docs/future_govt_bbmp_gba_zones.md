# Future — Bengaluru municipal areas (BBMP / GBA zones & wards)

> **REMINDER:** Pick this up after internal testing stabilizes and before a wide Bengaluru / BBMP pilot.  
> Linked from [future_features_backlog.md](future_features_backlog.md) §10.

Government + citizen alignment: improve **accountability routing**, **officer roster**, **map area coverage**, and match **current Karnataka municipal structure** for Bengaluru (citizen app + CG GOVT + Supabase seed).

---

## Current state (as of app v1.0.3)

| Layer | Official count | Citizen app | Gov app | Where in code |
|-------|----------------|-------------|---------|---------------|
| **GBA outer boundary (2025)** | 1 polygon | **Yes** | — | `BengaluruGbaBoundary.kt`, `assets/bengaluru_gba_boundary.json` |
| **GBA city corporations (2025)** | **5** | **Yes** (submit routing) | **No** — parked G2 | `MunicipalContactsRegistry.kt` → `gbaCorporationZones()` |
| BBMP zones (pre-2025 model) | **8** | lookup only (old reports) | legacy | `MunicipalContactsRegistry.kt` → `bbmpZones()` |
| GBA wards (2025) | **369** | **Yes** (submit only, not on map) | **No** — parked G5/G6 | `BengaluruGbaWards.kt`, `assets/bengaluru_gba_wards.json` |
| Zone head officers | 8 JC (legacy) + **5 named GBA corp commissioners** | seed + registry | seed | [`gba_official_contacts.md`](gba_official_contacts.md) — **G3 partially done**; ward officers still **G6** |
| Map locality labels | N/A (UX only) | ~200 neighborhoods | — | `HomeScreen.kt` → `bengaluruRegionLabelSpecs` |

**Citizen routing today (v1.0.3):** GPS → point-in-polygon **369 wards** → **5 GBA corporation** assignee + ward snapshot on report → Accountability tab + submit snackbar. Home map shows GBA red outline only (no corp/ward overlays).

**Government app:** Parked — see [government_app_handover.md](government_app_handover.md) § Government enhancements backlog.

**Why it can feel “incomplete” in the UI:**

1. Accountability tab lists **reports**, not a full zone/ward directory.
2. Map labels are **locality names**, not “East Zone / Bommanahalli Zone”.
3. Labels appear only above each spec’s `minZoom` — zoomed-out maps show fewer names.
4. **Central** and **North** as corporation names are **GBA 2025** — not in the old 8-zone BBMP model.

---

## Official reference — old BBMP (8 zones) — already in app

| # | Zone | Assignee key | Approx. center (routing) |
|---|------|--------------|--------------------------|
| 1 | East Zone | `BENGALURU:EAST` | 12.978, 77.640 |
| 2 | West Zone | `BENGALURU:WEST` | 13.010, 77.555 |
| 3 | South Zone | `BENGALURU:SOUTH` | 12.920, 77.585 |
| 4 | Mahadevapura Zone | `BENGALURU:MAHADEVAPURA` | 12.995, 77.715 |
| 5 | Bommanahalli Zone | `BENGALURU:BOMMANAHALLI` | 12.905, 77.625 |
| 6 | Rajarajeshwari Nagar Zone | `BENGALURU:RR_NAGAR` | 12.920, 77.520 |
| 7 | Dasarahalli Zone | `BENGALURU:DASARAHALLI` | 13.060, 77.530 |
| 8 | Yelahanka Zone | `BENGALURU:YELAHANKA` | 13.105, 77.600 |

No missing zones for this model. Officer names/addresses should be re-verified from BBMP/GBA public directories before production handover.

---

## Official reference — GBA (2025+) — structure + contacts

Karnataka replaced BBMP with the **Greater Bengaluru Authority (GBA)** and **five city corporations** (369 wards total).

| Corporation | HQ | Wards (approx.) |
|-------------|-----|-----------------|
| Bengaluru Central City Corporation | Hudson Circle / PUB | 63 |
| Bengaluru North City Corporation | Yelahanka | 72 |
| Bengaluru South City Corporation | Jayanagar | 72 |
| Bengaluru East City Corporation | Mahadevapura | 50 |
| Bengaluru West City Corporation | Rajarajeshwari Nagar | 112 |

**Commissioners (verified snapshot):** see **[`gba_official_contacts.md`](gba_official_contacts.md)** (as of **16 July 2026**). Wired into `MunicipalContactsRegistry.kt` + `supabase/seed/officers.json`.

**Ward officers:** not in app — legacy BBMP contact CSV kept under [`reference/`](reference/README.md) for research only.

**Data sources:**

- [GBA / BBMP portal](https://bbmp.gov.in/)
- [OpenCity — GBA Wards Delimitation 2025](https://data.opencity.in/dataset/gba-wards-delimitation-2025)
- Public transfer / press coverage listed in `gba_official_contacts.md`

---

## Implementation options (pick one path)

### Option A — Keep 8 BBMP zones (minimal change)

**When:** Internal pilot, demo to officials using current Joint Commissioner structure.

**Work:**

- [ ] Add citizen-facing **“BBMP zones”** info screen (list all 8 zones + officer title + office address; read-only).
- [ ] Show **zone name** on map at medium zoom (optional overlay), distinct from locality labels.
- [ ] Re-verify officer roster from BBMP/GBA site; update `MunicipalContactsRegistry.kt` + `officers.json` if names changed.
- [ ] Document in Accountability tab: “Routed to BBMP zone (not ward).”

**Effort:** Low (1–2 days).  
**Accuracy:** Zone-level only; fine for MVP.

---

### Option B — Migrate to 5 GBA corporations (recommended before public Bengaluru launch)

**When:** Aligning with 2025 GBA governance and officer structure.

**Work:**

- [ ] Replace 8 zone records with 5 corporation records in `MunicipalContactsRegistry.kt`.
- [ ] New assignee keys, e.g. `BENGALURU:GBA_CENTRAL`, `BENGALURU:GBA_NORTH`, … `BENGALURU:GBA_WEST`.
- [ ] Update corporation centroids (or rough bounding boxes) for `nearestAssignee`.
- [ ] Update `supabase/seed/officers.json` — commissioners / zonal heads per GBA corporation.
- [ ] Mirror in **government app** `MunicipalOfficersRegistry.kt` (separate project).
- [ ] Migration strategy for existing `reports.assignee_key` (map old 8 → new 5 or leave historical rows as-is).
- [ ] Update Accountability copy and Play Store / handover docs.

**Effort:** Medium (3–5 days + officer data gathering).  
**Accuracy:** Corporation-level; matches current Karnataka structure.

---

### Option C — Ward-level routing (369 GBA wards)

**When:** BBMP/GBA provides ward engineer contacts and you need ward-accurate accountability.

**Do not** hand-maintain 369 wards in Kotlin.

**Work:**

- [ ] Download official ward boundaries (KML/GeoJSON) from OpenCity dataset.
- [ ] Convert to lightweight polygons (simplify geometry for mobile).
- [ ] Ship as `assets/bengaluru_gba_wards.json` or load from Supabase `ward_boundaries` table.
- [ ] Implement point-in-polygon: report lat/lng → corporation + ward number + ward name.
- [ ] Store on report: `assignee_corp`, `assignee_zone` / `ward_number`, optional `ward_label`.
- [ ] Ward officer directory table (engineer name, phone if public) — source from GBA ward property lists / RTI directories.
- [ ] Fallback: corporation centroid if point outside all polygons (edge GPS error).
- [ ] Government app queue filters by ward/corporation.

**Effort:** High (2–4 weeks with boundary QA).  
**Accuracy:** Best for citizens and officers.

---

### Option D — Map locality labels only (no routing change)

**When:** Users want more neighborhood names on the map, not better government routing.

**Work:**

- [ ] Move `bengaluruRegionLabelSpecs` from `HomeScreen.kt` to `assets/bengaluru_locality_labels.json`.
- [ ] Add missing areas (user feedback list): e.g. Devanahalli, Sarjapur extension, Nelamangala town, etc.
- [ ] Tune `minZoom` so more labels appear at typical zoom levels without clutter.
- [ ] Optional: show current **BBMP/GBA zone** under locality name when GPS is known.

**Effort:** Low–medium.  
**Does not** fix ward-level accountability.

---

## Recommended phasing (updated July 2026)

```text
Done (v1.0.2)          → GBA outer boundary (#10.0): map outline, pan limits, submit validation
Next (v1.0.3 target)   → GBA corporation routing (#10.1–10.4): assignee keys → officers → UI labels
                         (Phase 3 backfill: decide before Supabase seed deploy)
Then                   → #17 heat map (needs corporation geometry from #10.1)
Parallel (optional)    → #14 locality labels, #10.6 zone reference UI (Option A)
Later                  → #10.5 ward polygons + ward officers (Option C)
```

### GBA corporation routing — detailed phases (#10.1–10.4)

| Phase | Work | Deliverables |
|-------|------|--------------|
| **1** | Add 5 GBA corporation assignee keys + centroids | `MunicipalContactsRegistry.kt`, `MunicipalAssignmentResolver.kt`; nearest-assignee uses corp centroids or rough corp bboxes |
| **2** | Update `officers.json` / Supabase seed with GBA commissioners | Verified names from BBMP/GBA directory; deploy seed; sync CG GOVT app registry |
| **3** | Re-map existing reports (optional backfill) or only new reports | **Decision:** SQL `UPDATE reports SET assignee_key = …` from lat/lng + old→new mapping, **or** forward-only (historical rows keep 8-zone keys) |
| **4** | Accountability UI labels (“East Corporation” vs “East Zone”) | `AccountabilitySection.kt`, `ReportDetailDialog.kt`, email/confirmation copy |

**Implementation order:** 1 → 2 → 4 in one citizen + gov release; confirm Phase 3 policy before Phase 2 deploy.

### Earlier options (still valid)

```text
Phase A (optional)      → Option A: zone/corp reference UI + copy clarity (#10.6)
Phase C (with GBA data) → Option C: ward polygons + ward officers (#10.5)
Parallel (optional)     → Option D: locality labels JSON (#14)
```

---

## Files to touch when implementing

| Area | Citizen app (`potholereport`) | Backend / gov |
|------|------------------------------|---------------|
| Zone/corp definitions | `data/MunicipalContactsRegistry.kt` | — |
| GPS → assignee | `data/MunicipalAssignmentResolver.kt` | — |
| Report fields | `data/RecentReportsRepository.kt`, `remote/RemoteReportRow.kt` | `reports` columns already have `assignee_*` |
| UI | `ui/home/AccountabilitySection.kt`, `ReportDetailDialog.kt`, optional new zones screen | — |
| Officers | — | `supabase/seed/officers.json`, `seed_officers.mjs` |
| Gov routing | — | `Potholegovt` → `MunicipalOfficersRegistry.kt` |

---

## Acceptance criteria (when done)

- [ ] Citizen sees **correct municipal unit** (zone, corporation, or ward) for report GPS.
- [ ] Accountability tab explains **source** (BBMP/GBA public directory).
- [ ] Government app receives same `assignee_key` / ward metadata on sync.
- [ ] Bengaluru map feels complete at common zoom levels (if Option D included).
- [ ] Officer roster verified against official site within 6 months of release.

---

## Reminder checklist (for you)

Use this when you return to this task:

- [ ] Confirm with stakeholder: **8 zones vs 5 GBA corporations vs 369 wards**
- [ ] Download latest ward KML from OpenCity if choosing Option C
- [ ] Re-read this doc and [government_app_handover.md](government_app_handover.md) § officers
- [ ] Update citizen + government apps in the **same release** if assignee keys change
- [ ] Run smoke test: report in Koramangala, Whitefield, Yelahanka, RR Nagar → correct assignee

---

*Drafted: June 2026 — from Play internal testing / BBMP coverage review.*
