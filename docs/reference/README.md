# Reference data (not runtime assets)

Files here are for research / PPT / officer verification. They are **not** loaded by the Android app.

## `legacy_bbmp_ward_officers_contacts.csv`

- Source: [OpenCity — Bengaluru BBMP Officers Contact Numbers by ward](https://data.opencity.in/dataset/bbmp-ward-information) (resource updated ~Nov 2025).
- Structure: legacy **BBMP** wards (JC / DC / EE / AEE / AE / health / RO…), **not** the GBA 369-ward officer directory.
- **Do not** map these rows 1:1 onto GBA wards or corporation commissioners without a verified remapping.
- App ward routing uses `assets/bengaluru_gba_wards.json` for ward **names/geometry** only.

Verified GBA commissioner names live in [`../gba_official_contacts.md`](../gba_official_contacts.md).
