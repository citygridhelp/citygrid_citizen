# City Grid — Play Store feature graphic

**Size:** 1024 × 500 px  
**Format:** JPG or 24-bit PNG (no transparency)  
**Upload:** Play Console → **Grow users** → **Store presence** → **Main store listing** → **Feature graphic**

---

## Brand reference (from repo)

| Asset | Path |
|-------|------|
| App icon (master) | `app/src/main/res/drawable-nodpi/city_grid_icon_short.png` |
| Splash / brand | `app/src/main/res/drawable-nodpi/city_grid_splash.png` |
| Report button red | `#B74233` |
| Grid cyan glow | `#00D4FF` / cyan-teal |
| Dark background | `#0D0D12` – `#1A1A2E` |

---

## Layout

```text
┌──────────────────────────────────────────────────────────────────┐ 1024 × 500
│  ┌─────────┐   Report potholes -                                 │
│  │  City   │   anonymously                                       │
│  │  Grid   │   ─── red accent #B74233                            │
│  │  icon   │   Track your reports on the map                     │
│  └─────────┘   City Grid — for Citizens                            │
│  solid dark background (no grid overlay on banner)                 │
└──────────────────────────────────────────────────────────────────┘
```

---

## AI image prompt (Canva AI / DALL·E / Midjourney / Leonardo)

Copy and adjust if needed:

```text
Google Play feature graphic banner, 1024x500 pixels, landscape. Civic technology app called City Grid.

Left side: square app icon on dark charcoal background, white bold text "City" above "Grid", glowing cyan digital city grid lines and network nodes around it (futuristic map aesthetic).

Right side: marketing text on dark navy-black background with subtle cyan grid lines:
- Headline (white, bold, two lines): "Report potholes -" / "anonymously"
- Subline (white): "Track your reports on the map"
- Tagline (light cyan or gray): "City Grid — for Citizens"

**Logo on left:** Use the real app asset (`city_grid_icon_short.png`) — do **not** add an extra cyan square frame around it. The icon’s grid should blend into the banner background without a second enclosing border.
- Small terracotta red accent #B74233 as underline or dot

Style: clean, professional, no phone mockup, no screenshots, no clutter, high contrast, readable typography, solid background edge to edge, Play Store marketing quality.
```

**Midjourney:** add `--ar 1024:500` or `--ar 2:1`  
**DALL·E / ChatGPT:** ask for “exactly 1024 wide by 500 tall pixels” and regenerate if aspect ratio is wrong.

---

## Canva (manual, recommended for exact size)

1. [Canva](https://www.canva.com) → search **“Google Play Feature Graphic”** (1024 × 500 template).
2. Background: dark gradient `#0D0D12` → `#1A1A2E`.
3. **Upload** `city_grid_icon_short.png` → place left (~180–220 px wide), vertically centered.
4. Add faint cyan line/grid elements (search “tech grid” elements) at ~15% opacity.
5. Text (right side, left-aligned):
   - **Report potholes -** / **anonymously** — white, bold, two lines ~36 pt
   - **Track your reports on the map** — white, regular, ~24–28 pt
   - **City Grid — for Citizens** — cyan-gray, ~18–20 pt
6. Optional: 4 px line under headline in `#B74233`.
7. **Download** → PNG or JPG → verify **1024 × 500** before upload.

---

## Photopea (free, exact pixels)

1. [photopea.com](https://www.photopea.com) → **File → New** → **1024 × 500** px.
2. Follow same layout as Canva steps above.

---

## Ready-to-upload file (this repo)

```text
docs/store_assets/city_grid_feature_graphic_1024x500.png
```

Dimensions verified: **1024 × 500 px**. Upload directly to Play Console, or tweak text in Canva/Photopea if you want changes.

---

- [ ] Exactly **1024 × 500** px  
- [ ] No transparent edges (use solid dark fill)  
- [ ] Text readable at thumbnail size  
- [ ] Matches City Grid icon branding  
- [ ] No misleading claims / no “#1 app” badges  

---

## Play Console upload

1. Play Console → your app **City Grid**
2. **Grow users** → **Store presence** → **Main store listing**
3. **Feature graphic** → Upload `city_grid_feature_graphic_1024x500.png`
4. **Save** → review preview on phone/tablet mockup in Console
