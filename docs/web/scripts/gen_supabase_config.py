#!/usr/bin/env python3
"""Write docs/web/assets/supabase-config.js from repo-root local.properties."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
props_path = ROOT / "local.properties"
out = ROOT / "docs" / "web" / "assets" / "supabase-config.js"

props = {}
for line in props_path.read_text(encoding="utf-8").splitlines():
    line = line.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    k, v = line.split("=", 1)
    props[k.strip()] = v.strip()

url = props.get("SUPABASE_URL", "")
key = props.get("SUPABASE_ANON_KEY", "")
if not url or not key:
    raise SystemExit("local.properties missing SUPABASE_URL or SUPABASE_ANON_KEY")

out.write_text(
    "/** Generated from local.properties — do not commit. */\n"
    "window.CG_SUPABASE = {\n"
    f"  url: {url!r},\n"
    f"  anonKey: {key!r}\n"
    "};\n",
    encoding="utf-8",
)
print(f"Wrote {out}")
