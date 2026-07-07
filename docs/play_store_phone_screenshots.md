# City Grid — Play Store phone screenshots

Guide for **Default store listing → Phone screenshots** in Google Play Console.

Related: [play_store_feature_graphic.md](play_store_feature_graphic.md) · [release_and_versioning_guide.md](release_and_versioning_guide.md)

---

## Play Console requirements

| Rule | Value |
|------|--------|
| **Minimum** | **2** screenshots |
| **Maximum** | **8** |
| **Aspect ratio** | **9:16** portrait (recommended) or 16:9 |
| **Size** | Short side ≥ 320 px, long side ≤ 3840 px |
| **Recommended** | **1080 × 1920** or **1080 × 2340** px |
| **Format** | PNG or JPEG |
| **Content** | Must be **real app UI** (not marketing mockups only) |

Upload: **Grow users** → **Store presence** → **Main store listing** → **Phone screenshots**

---

## Recommended set (4 screenshots)

Capture in this **order** (first image = most visible in store):

| # | Screen | Why | How to reach |
|---|--------|-----|----------------|
| **1** | **Home — map + Report CTA** | Shows core value: city map + report potholes | Open app → Home tab → Bengaluru selected → map with clusters visible |
| **2** | **New report — report flow** | Shows anonymous reporting, photos, lane | Tap **Report a pothole** → New report screen with close-up step (photos optional if empty) |
| **3** | **My Reports — list** | Signed-in benefit: track submissions | Sign in → scroll to **My Reports** strip or section |
| **4** | **Report detail** | Status, photos, location, accountability | My Reports → tap a report → detail dialog |

**Minimum for internal testing:** screenshots **#1** and **#2** only.

**Optional 5–8:** Recent reports on map · Profile (hide email) · Wide-road capture step · Severity / lane selector on new report.

---

## Before you capture

- [ ] Use a **release** or **debug** build that matches what you ship (`in.citygrid.citizen`).
- [ ] **Bengaluru** selected (or your launch city) so map labels look relevant.
- [ ] **Location** allowed (or pick map position) so the map is not empty.
- [ ] **Hide PII:** no real email in profile; blur or use a test account (`citygridhelp@gmail.com` is OK if you’re comfortable).
- [ ] **Status bar:** full battery / signal looks better; avoid low-battery icon if possible.
- [ ] **Light theme** (app default) — consistent across all shots.
- [ ] If new report footer still says old copy, OK for now; **#20** backlog updates to “REPORT ANONYMOUSLY · NO IP STORED”.

---

## Option A — Android Studio emulator (recommended)

### 1. Create device

1. **Device Manager** → **Create device**
2. Phone → **Pixel 7** or **Pixel 6** (1080 × 2400)
3. System image: latest stable API (e.g. API 34/35)
4. Finish → **Run** app on this AVD

### 2. Capture each screen

1. Navigate to the screen (see table above).
2. Emulator toolbar → **⋯** → **Screenshot**  
   Or: **View** → **Tool Windows** → **Logcat** area → camera icon on emulator panel.
3. Saves PNG (often ~1080 × 2400) — good for Play.

### 3. Save files

```text
docs/store_assets/phone/
  01_home_map.png
  02_new_report.png
  03_my_reports.png
  04_report_detail.png
```

---

## Option B — Physical phone

1. Install debug or internal-test APK/AAB.
2. Open each screen.
3. **Power + Volume down** (most Android devices).
4. Transfer PNGs to PC → rename as above.
5. If resolution differs, resize only if needed (keep 9:16); Play accepts many phone sizes.

---

## Option C — `adb` (scriptable)

With device/emulator connected:

```powershell
cd c:\Users\priye\AndroidStudioProjects\potholereport
New-Item -ItemType Directory -Force -Path docs\store_assets\phone | Out-Null

# Navigate to screen on device, then run for each:
adb exec-out screencap -p > docs\store_assets\phone\01_home_map.png
```

Repeat after changing screens (`02_…`, `03_…`, etc.).

---

## Upload in Play Console

1. **Main store listing** → **Phone screenshots**
2. **Upload** (drag folder or pick files)
3. Order: **01** first (hero), then 02, 03, 04
4. Reorder with drag handles if needed
5. **Save** → check **Preview** on the right

---

## Shot-by-shot tips

### 1 — Home map

- Zoom so **cluster numbers** or **report pins** are visible.
- Show **Report a pothole** button.
- Optional: GPS strip showing accuracy (not “searching…”).
- City name / Bengaluru visible if your UI shows it.

### 2 — New report

- Show **close-up** step: lane tiles (L/M/R), severity, camera area.
- If photos empty, still OK — shows the flow.
- Avoid error toasts (“photo rejected”) in the shot.

### 3 — My Reports

- Requires **signed-in** test user with at least one report.
- Show **status** (Open / In progress) and thumbnail if available.

### 4 — Report detail

- Open from My Reports.
- Show map pin, severity, area label, status — not reporter email.

---

## What not to upload

- Screenshots with **login passwords** visible
- **Placeholder** or lorem ipsum
- **Other apps’** UI or stock photos only
- Heavily **framed** marketing images (Play wants actual UI; device frames optional in Console)

---

## Checklist

- [ ] At least **2** phone screenshots uploaded
- [ ] Portrait **9:16**
- [ ] Real app screens from City Grid citizen build
- [ ] Order tells a story: map → report → track → detail
- [ ] No sensitive personal data visible
- [ ] Matches feature graphic message (“report anonymously”)

---

## After phone screenshots

| Next (optional) | Required? |
|-----------------|-----------|
| **7" / 10" tablet** screenshots | No — skip for first internal test |
| **Short description** (80 chars) | Yes |
| **Full description** | Yes |
| **Promo video** (YouTube) | No |

When screenshots are saved under `docs/store_assets/phone/`, add that folder to `.gitignore` if you prefer not to commit large PNGs (optional).
