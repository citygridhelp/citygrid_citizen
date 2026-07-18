#!/usr/bin/env python3
"""Regenerate app/src/main/assets/bengaluru_gba_boundary.json from OpenCity GBA KML."""

from __future__ import annotations

import json
import math
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KML_LOCAL = ROOT / "tools" / "gba_boundary_sept_2025.kml"
KML_URL = (
    "https://data.opencity.in/dataset/greater-bengaluru-authority-corporations-delimitation-2025/"
    "resource/a8a9de49-7455-44bc-80dc-e6640014ac19/download/gba_boundary_sept_2025.kml"
)
OUT = ROOT / "app" / "src" / "main" / "assets" / "bengaluru_gba_boundary.json"
SIMPLIFY_EPS = 0.0012  # degrees (~130 m)


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


def load_kml(path: Path) -> list[tuple[float, float]]:
    root = ET.parse(path).getroot()
    best: list[tuple[float, float]] = []
    for coords_el in root.iter("{http://www.opengis.net/kml/2.2}coordinates"):
        text = (coords_el.text or "").strip()
        pts: list[tuple[float, float]] = []
        for tok in text.split():
            parts = tok.split(",")
            if len(parts) >= 2:
                pts.append((float(parts[1]), float(parts[0])))
        if len(pts) > len(best):
            best = pts
    if not best:
        raise SystemExit("No coordinates found in KML")
    if best[0] != best[-1]:
        best.append(best[0])
    return best


def main() -> None:
    if not KML_LOCAL.exists():
        print(f"Downloading KML to {KML_LOCAL} …")
        KML_LOCAL.parent.mkdir(parents=True, exist_ok=True)
        urllib.request.urlretrieve(KML_URL, KML_LOCAL)
    outer = load_kml(KML_LOCAL)
    simplified = rdp(outer, SIMPLIFY_EPS)
    lats = [p[0] for p in simplified]
    lons = [p[1] for p in simplified]
    payload = {
        "source": "Greater Bengaluru Authority boundary map, September 2025 (OpenCity / bbmp.gov.in/gisviewer)",
        "sourceDataset": "https://data.opencity.in/dataset/greater-bengaluru-authority-corporations-delimitation-2025",
        "license": "Public Domain (OpenCity)",
        "bboxNorth": round(max(lats), 6),
        "bboxEast": round(max(lons), 6),
        "bboxSouth": round(min(lats), 6),
        "bboxWest": round(min(lons), 6),
        "ringLatLon": [[round(lat, 6), round(lon, 6)] for lat, lon in simplified],
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, separators=(",", ":")), encoding="utf-8")
    print(f"Wrote {OUT} ({len(simplified)} vertices, was {len(outer)})")
    print(
        f"BBox N={payload['bboxNorth']} E={payload['bboxEast']} "
        f"S={payload['bboxSouth']} W={payload['bboxWest']}"
    )


if __name__ == "__main__":
    main()
