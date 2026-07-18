# Play Store — Main store listing copy

**App:** City Grid (`in.citygrid.citizen`)  
**Upload:** Play Console → **Grow users** → **Store presence** → **Main store listing**

Bengaluru pilot — store listing v1.0.2 (July 2026). Updated for Google Play **Misleading Claims** policy.

---

## Short description (en-US) — max 80 characters

```
Report Bengaluru potholes anonymously. Independent app — not government.
```

(69 characters)

---

## Full description (en-US) — copy from line below through end of block

**~2,450 characters** (Play limit: 4,000)

```text
DISCLAIMER
City Grid (citygridhelp@gmail.com) is an independent civic reporting app. It is NOT affiliated with, endorsed by, or operated by BBMP, any municipal corporation, or any emergency authority. Officer names and zones are copied from publicly published official directories for citizen reference only. City Grid does not guarantee government action or repair timelines.

City Grid helps Bengaluru citizens report potholes and road hazards while keeping identity private on public maps and shared reports. More Indian cities will be added when official municipal directories are verified.

GOVERNMENT DATA SOURCES (complete list for this app)
• Bengaluru BBMP zone officers: https://bbmp.gov.in/
• India emergency 112, 100, 101, 102, 108: https://www.india.gov.in/directory/helpline
• India ERSS / 112 system (Ministry of Home Affairs): https://www.mha.gov.in/en/commoncontent/emergency-response-support-system-erss
• Weather on home screen (third-party, not government): https://open-meteo.com/
• Privacy policy: https://citygridhelp.github.io/citygrid_citizen/privacy_policy

Note: Municipal zone officers are shown for Bengaluru (BBMP) only in this release. Other cities appear in the city picker as coming soon.

WHY CITY GRID
• Report anonymously — your name is not shown on public maps; private reporter ID for tracking
• My Reports — sign in to track Open, In Progress, Completed with photos, location, severity, status
• BBMP zone accountability — Bengaluru reports tagged with municipal zone and public officer from https://bbmp.gov.in/ (reference only; not an official government submission portal)
• Bengaluru map — interactive map, GPS, place search, recent reports, severity at a glance

HOW REPORTING WORKS
1. Sign in with email (quick verification)
2. Report a Pothole — close-up and wide photos, GPS, severity (Minor to Critical), lane position
3. On-device checks help confirm a real road hazard before submit
4. Submit through City Grid for civic tracking; ticket reference and status updates when available

MY REPORTS
Dashboard for every submission: Open (received), In Progress (work started), Completed (repair finished). Evidence photos, location, notes, timestamps. Pull to refresh for latest status.

MAP & MORE
• Bengaluru pothole map with severity clusters
• Accountability tab — BBMP zone officers from https://bbmp.gov.in/
• Weather · emergency quick-dial (sources above) · profile · notifications · light/dark theme

PRIVACY
Email and account stay private on shared views. Only anonymous reporter ID appears publicly. Location and camera used only to place and document reports.

Download City Grid. Report what you see. Track the fix.

Note: Independent civic tool for Bengaluru in this release. We do not control BBMP repair schedules. For emergencies, call official services (sources above).
```

---

## App changes in v1.0.1 (versionCode 2)

| Change | File |
|--------|------|
| Bengaluru-only enabled city | `CityLaunchConfig.kt` |
| Other cities grayed in picker | `HomeScreen.kt` |
| BBMP source link in Accountability | `AccountabilitySection.kt` |
| Disclaimer in About + Emergency | `HomeScreen.kt`, `EmergencyScreen.kt` |
| BBMP-only municipal assignee routing | `MunicipalAssignmentResolver.kt` |

## App changes in v1.0.2 (versionCode 3) — planned

Full user-facing copy, implementation table, map layout, and upload checklist:

**[play_store_release_notes_1.0.2.md](play_store_release_notes_1.0.2.md)**

## Before uploading v1.0.2

1. Build `bundleRelease` with versionCode **3** / versionName **1.0.2** (already set in `app/build.gradle.kts`)
2. Upload AAB to Closed testing (or your review track)
3. **Release name:** `1.0.2 — GBA map, notifications, severity filter`
4. Paste **What's New** from [play_store_release_notes_1.0.2.md](play_store_release_notes_1.0.2.md)
5. **Main store listing** — only re-paste Full/Short description if copy changed (not required for every release)
6. **App content** → Government app = **No**
7. Open each URL in incognito — must load
8. **Publishing overview** → Send for review / Start rollout

For v1.0.1 upload steps (historical): versionCode **2** / versionName **1.0.1**

See also: [privacy_policy_hosting.md](privacy_policy_hosting.md)
