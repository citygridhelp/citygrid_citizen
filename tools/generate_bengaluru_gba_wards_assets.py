#!/usr/bin/env python3
"""Regenerate app/src/main/assets/bengaluru_gba_wards.json from OpenCity GBA ward KML."""

from __future__ import annotations

import json
import math
import re
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KML_LOCAL = ROOT / "tools" / "gba_wards_369.kml"
KML_URL = (
    "https://data.opencity.in/dataset/gba-wards-delimitation-2025/"
    "resource/dbe57ae6-a72f-426e-917b-e55b3b22a87b/download/gba_final_wards_369_wards.kml"
)
OUT = ROOT / "app" / "src" / "main" / "assets" / "bengaluru_gba_wards.json"
NS = {"k": "http://www.opengis.net/kml/2.2"}
SIMPLIFY_EPS = 0.00025  # ~25 m — tighter than city boundary for ward borders

CORP_KEYS = {
    "Central": ("BENGALURU:GBA_CENTRAL", "Bengaluru Central City Corporation"),
    "East": ("BENGALURU:GBA_EAST", "Bengaluru East City Corporation"),
    "North": ("BENGALURU:GBA_NORTH", "Bengaluru North City Corporation"),
    "South": ("BENGALURU:GBA_SOUTH", "Bengaluru South City Corporation"),
    "West": ("BENGALURU:GBA_WEST", "Bengaluru West City Corporation"),
}


def perp_dist(point, start, end):
    lat, lon = point
    lat1, lon1 = start
    lat2, lon2 = end
    dx, dy = lat2 - lat1, lon2 - lon1
    if dx == 0 and dy == 0:
        return math.hypot(lat - lat1, lon - lon1)
    t = max(0.0, min(1.0, ((lat - lat1) * dx + (lon - lon1) * dy) / (dx * dx + dy * dy)))
    return math.hypot(lat - (lat1 + t * dx), lon - (lon1 + t * dy))


def rdp(points, eps):
    if len(points) < 3:
        return points
    start, end = points[0], points[-1]
    max_d, idx = 0.0, 0
    for i in range(1, len(points) - 1):
        d = perp_dist(points[i], start, end)
        if d > max_d:
            max_d, idx = d, i
    if max_d > eps:
        left = rdp(points[: idx + 1], eps)
        right = rdp(points[idx:], eps)
        return left[:-1] + right
    return [start, end]


def parse_coords(text: str) -> list[tuple[float, float]]:
    pts: list[tuple[float, float]] = []
    for tok in text.split():
        parts = tok.split(",")
        if len(parts) >= 2:
            pts.append((float(parts[1]), float(parts[0])))
    if pts and pts[0] != pts[-1]:
        pts.append(pts[0])
    return pts


def simple_data(placemark: ET.Element) -> dict[str, str]:
    out: dict[str, str] = {}
    for sd in placemark.findall(".//k:SimpleData", NS):
        name = sd.get("name")
        if name:
            out[name] = (sd.text or "").strip()
    return out


def ward_number_from(meta: dict[str, str]) -> int:
    raw = meta.get("ward_id", "").strip()
    if raw.isdigit():
        return int(raw)
    m = re.search(r"(\d+)", meta.get("Ward_Name", ""))
    return int(m.group(1)) if m else 0


def main() -> None:
    if not KML_LOCAL.exists():
        print(f"Downloading KML to {KML_LOCAL} …")
        KML_LOCAL.parent.mkdir(parents=True, exist_ok=True)
        urllib.request.urlretrieve(KML_URL, KML_LOCAL)

    root = ET.parse(KML_LOCAL).getroot()
    wards_out: list[dict] = []
    for pm in root.findall(".//k:Placemark", NS):
        meta = simple_data(pm)
        corp_short = meta.get("Corporation", "").strip()
        if corp_short not in CORP_KEYS:
            continue
        corp_key, corp_label = CORP_KEYS[corp_short]
        ward_num = ward_number_from(meta)
        ward_name = meta.get("ward_name", "").strip() or meta.get("Ward_Name", "").strip()
        if not ward_num or not ward_name:
            continue
        coords_el = pm.find(".//k:coordinates", NS)
        if coords_el is None or not (coords_el.text or "").strip():
            continue
        outer = parse_coords(coords_el.text)
        if len(outer) < 4:
            continue
        ring = rdp(outer, SIMPLIFY_EPS)
        lats = [p[0] for p in ring]
        lons = [p[1] for p in ring]
        ward_key = f"{corp_key}:W{ward_num:03d}"
        wards_out.append(
            {
                "wardKey": ward_key,
                "wardNumber": ward_num,
                "wardName": ward_name,
                "corporationKey": corp_key,
                "corporationLabel": corp_label,
                "corporationShort": corp_short,
                "bboxNorth": round(max(lats), 6),
                "bboxSouth": round(min(lats), 6),
                "bboxEast": round(max(lons), 6),
                "bboxWest": round(min(lons), 6),
                "ringLatLon": [[round(lat, 6), round(lon, 6)] for lat, lon in ring],
            }
        )

    wards_out.sort(key=lambda w: (w["corporationKey"], w["wardNumber"]))
    if len(wards_out) < 360:
        raise SystemExit(f"Expected ~369 wards, got {len(wards_out)}")

    payload = {
        "source": "GBA Final Wards Map — 369 wards (OpenCity / Nov 2025 notification)",
        "sourceDataset": "https://data.opencity.in/dataset/gba-wards-delimitation-2025",
        "license": "Public Domain (OpenCity)",
        "wardCount": len(wards_out),
        "wards": wards_out,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, separators=(",", ":")), encoding="utf-8")
    print(f"Wrote {OUT} ({len(wards_out)} wards)")


if __name__ == "__main__":
    main()
