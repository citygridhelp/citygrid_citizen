# Map chrome UI backup — before redesign (A/B/C)

**Created:** 2026-08-11  
**Purpose:** Restore the current Severity / Trip / Heat / Weather bar if a redesign is not preferred.  
**Branch tip:** `backup/map-chrome-ui-before-redesign` (points at the commit that added this folder)

## What is “current” UI

Home map filter row (not fullscreen):

- **Severity** dropdown (ALL / Minor / …)
- **Trip** chip
- **Heat** chip (Bengaluru)
- **Weather** chip + **1h · 3h · Today** when Weather is on
- Heat XOR Weather

## Files in this folder

| File | Role |
|------|------|
| `HomeScreen.kt` | Full home screen (includes the chrome bar) — **primary restore** |
| `MapChromeBar.snippet.kt` | Just the chrome bar block (~lines 2106–2345) for quick diff |
| `CitizenChrome.kt` | Shared chrome tokens used by chips |
| `WeatherHorizon.kt` | Horizon enum |
| `CityWeatherRepository.kt` | Rain grid fetch |
| `CityWeatherRainGrid.kt` | Cells + colors |
| `CityWeatherRainOverlay.kt` | OSM rain overlay |

MapLibre weather layer lives in `MapLibreDensityMap.kt` (not copied; restore HomeScreen first for chrome).

## How to roll back the chrome UI

### Option 1 — restore full HomeScreen from this backup

From repo root (PowerShell):

```powershell
Copy-Item -Force "docs/ui_backups/map_chrome_before_redesign_20260811/HomeScreen.kt" `
  "app/src/main/java/com/example/potholereport/ui/home/HomeScreen.kt"
```

Rebuild the app. Use this if the redesign touched a lot of `HomeScreen.kt`.

### Option 2 — git restore of this backup commit’s snapshot

```powershell
git show backup/map-chrome-ui-before-redesign:docs/ui_backups/map_chrome_before_redesign_20260811/HomeScreen.kt `
  | Set-Content -Encoding utf8 "app/src/main/java/com/example/potholereport/ui/home/HomeScreen.kt"
```

### Option 3 — surgical

Diff `MapChromeBar.snippet.kt` against the live `HomeScreen.kt` chrome block and paste the snippet back.

## Note

This does **not** freeze the entire WIP working tree—only the map-chrome–related UI snapshot above. Other uncommitted work stays as-is.
