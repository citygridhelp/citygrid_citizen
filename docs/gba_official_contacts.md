# GBA official contacts (verified from public sources)

**As of:** 16 July 2026  
**Purpose:** Source of truth for citizen-app commissioner names and government seed placeholders.  
**Not a live scrape.** Names change on IAS transfer orders — re-verify before production claims.

Related: [`future_govt_bbmp_gba_zones.md`](future_govt_bbmp_gba_zones.md) · [`MunicipalContactsRegistry.kt`](../app/src/main/java/com/example/potholereport/data/MunicipalContactsRegistry.kt) · [`supabase/seed/officers.json`](../supabase/seed/officers.json)

---

## Coverage status

| Layer | In docs | Wired in citizen registry | In `officers.json` seed | Notes |
|-------|---------|---------------------------|-------------------------|--------|
| GBA Chief Commissioner | Yes | HQ fallback | Yes (`COMMISSIONER`) | Public listings + Jul 2026 press |
| GBA Special Commissioners | Yes | No (coordination roles, not report assignees) | No | Listed for PPT / handover only |
| 5 City Corporation Commissioners | Yes | Yes (`GBA_*` assignee keys) | Yes (`ZONE_HEAD`) | Central updated Jun 2026 |
| Additional Commissioners (per corp) | Yes (where known) | No | No | Not used for pothole assignee keys yet |
| 369 GBA ward officers | **No** | **No** | **No** | No stable public machine-readable GBA ward-officer directory; OpenCity CSV is **legacy BBMP** wards — do not treat as GBA |

---

## Greater Bengaluru Authority (apex)

| Role | Name | Source notes |
|------|------|----------------|
| Chief Commissioner (Member Secretary) | **Sri M. Maheshwar Rao, IAS** | GBA/BBMP portal “GBA Officers”; still in Jul 2026 civic press (e.g. TNIE 15 Jul 2026) |
| Special Commissioner (Revenue & IT) | **Sri Munish Moudgil, IAS** | Portal / early GBA reconstitution reporting — **re-check portal before PPT if citing** |
| Special Commissioner (Admin / Finance / DM / CFO) | **Dr. Harish Kumar K., IAS** | Portal listings (titles have shifted across 2025–26 orders) |
| Special Commissioner (FECC / Election / PR) | **Smt. Sushama Godbole, IAS** | Portal “GBA Officers” (as published on site) |
| Special Commissioner (Welfare / Health / Education) | **Sri Venkatachalapathy R., IAS** | Portal “GBA Officers” (as published on site) |

Office (HQ fallback used in app): BBMP / GBA Head Office, N.R. Square, Bengaluru, Karnataka 560002 · public email often cited: `comm@bbmp.gov.in`

---

## Five City Corporation Commissioners (report assignees)

Wired to citizen routing via `BENGALURU:GBA_*` keys.

| Assignee key | Corporation | Commissioner (as of 16 Jul 2026) | HQ (app address) | Evidence |
|--------------|-------------|----------------------------------|------------------|----------|
| `BENGALURU:GBA_CENTRAL` | Bengaluru Central City Corporation | **Sri G. Jagadeesha, IAS** | PUB / Hudson Circle area (Central HQ) | GoK transfer order covered by *The Hindu* (5 Jun 2026); took charge ~6 Jun 2026. **Replaces** P. Rajendra Cholan (Sep 2025 inaugural posting). |
| `BENGALURU:GBA_EAST` | Bengaluru East City Corporation | **Sri Ramesh D.S., IAS** | Mahadevapura | Sep 2025 GBA corp appointments; still cited Apr 2026 |
| `BENGALURU:GBA_NORTH` | Bengaluru North City Corporation | **Sri Pommala Sunil Kumar, IAS** | Yelahanka | Sep 2025 appointments; still cited Apr 2026 |
| `BENGALURU:GBA_SOUTH` | Bengaluru South City Corporation | **Sri Ramesh K.N., IAS** | Jayanagar / South | Sep 2025 appointments; still cited Apr 2026 |
| `BENGALURU:GBA_WEST` | Bengaluru West City Corporation | **Dr. Rajendra K.V., IAS** | Rajarajeshwari Nagar | Sep 2025 appointments; West budget Mar 2026 (*The Hindu* / TNIE) names K.V. Rajendra as Commissioner |

### Additional Commissioners (Development) — inaugural Sep 2025 set

Not assignee keys in the citizen app. Recorded for completeness; **may have changed**.

| Corporation | Additional Commissioner (Development) |
|-------------|----------------------------------------|
| Central | Rahul Sharanappa Sankanur |
| East | Lokhande Snehal Sudhakar |
| North | Latha R. |
| South | Pandve Rahul Tukaram |
| West | Digvijay Bodke |

---

## Ward officers (369)

**Not implemented.** Reasons:

1. GBA ward engineer / health inspector contacts are not published as a single maintained open dataset aligned to the **369** wards.
2. OpenCity “BBMP Officers Contact Numbers by ward” is a **legacy BBMP** sheet (old ward numbering). Mapping it blindly onto GBA wards would show wrong names.
3. Product rule remains: citizen routing stops at **corporation commissioner** + ward **name/number** on the report — ward officers are gov-app backlog **G6**.

When a public GBA ward roster appears (PDF/CSV on bbmp.gov.in or UDD), ingest that file — do not scrape HTML as the pipeline.

---

## Source list (cite in PPT / audits)

1. [bbmp.gov.in](http://bbmp.gov.in/home) — GBA Officers block  
2. *The Hindu* — Central Commissioner G. Jagadeesha appointment (5 Jun 2026)  
3. Sep 2025 GoK / GBA corporation commissioner postings (Indian Masterminds / eGov / contemporaneous press)  
4. West City Corporation budget coverage Mar 2026 — Commissioner K.V. Rajendra  
5. TNIE — Maheshwar Rao still Chief Commissioner (15 Jul 2026)  
6. GBA reconstitution member lists (e.g. Feb 2026 UDD notification summaries) — cross-check for Mayors / ex-officio only  

---

## Maintenance

- After any IAS reshuffle affecting Bengaluru corps: update this file, then `MunicipalContactsRegistry.kt` + `officers.json`, then re-seed gov logins if emails stay the same.
- Never claim “scraped live from GBA” in the app UI — show names as **directory snapshot** with an implicit or explicit “verify with corporation” tone if needed.
