# City Grid citizen app — Git branching & versioning

Git workflow for **`citygridhelp/citygrid_citizen`** (`potholereport` local folder).

Related: [release_and_versioning_guide.md](release_and_versioning_guide.md) — Play Store, `versionCode` / `versionName`, Supabase migrations, testing.

*Last reviewed: July 2026*

---

## Branch map

| Environment | Branch | GitHub remote | Purpose |
|-------------|--------|---------------|---------|
| **Development** | `dev` | `origin/dev` | Daily work, feature fixes, internal testing |
| **UAT** | `uat` | `origin/uat` | User acceptance / pre-production verification |
| **Production** | `master` | `origin/master` | Live / Play Store production line |

**Remote:** `https://github.com/citygridhelp/citygrid_citizen.git`

**Note:** This repo also has `origin/stage` from an older setup. Use **`uat`** for UAT going forward, or treat `stage` as UAT if you prefer — pick one and stick to it.

---

## Flow overview

```text
dev  ──(test OK)──►  uat  ──(UAT OK)──►  master (prod)
```

Always work on **`dev`**. Promote to **`uat`** only after dev testing. Promote to **`master`** only after UAT sign-off.

---

## 1. Daily work → local `dev` + GitHub `dev`

```powershell
cd c:\Users\priye\AndroidStudioProjects\potholereport

git checkout dev
git pull origin dev

git add .
git status
git commit -m "Describe your change"
git push origin dev
```

**First push** to GitHub `dev` (set upstream once):

```powershell
git push -u origin dev
```

---

## 2. Promote `dev` → UAT (GitHub + local)

### First time only — create `uat` from `dev`

```powershell
cd c:\Users\priye\AndroidStudioProjects\potholereport

git checkout dev
git pull origin dev
git checkout -b uat
git push -u origin uat
```

### Every UAT release (after dev is verified)

```powershell
cd c:\Users\priye\AndroidStudioProjects\potholereport

git checkout dev
git pull origin dev

git checkout uat
git pull origin uat
git merge dev
git push origin uat

git checkout dev
```

---

## 3. Promote UAT → production (`master`)

After UAT is verified:

```powershell
cd c:\Users\priye\AndroidStudioProjects\potholereport

git checkout uat
git pull origin uat

git checkout master
git pull origin master
git merge uat
git push origin master

git checkout dev
```

**Safer option:** open a **Pull request** on GitHub (`uat` → `master`), review, then merge instead of merging locally.

---

## Quick cheat sheet

```powershell
# Dev — commit and push
git checkout dev
git add .
git commit -m "Your message"
git push origin dev

# Dev → UAT
git checkout uat
git pull origin uat
git merge dev
git push origin uat
git checkout dev

# UAT → Prod
git checkout master
git pull origin master
git merge uat
git push origin master
git checkout dev
```

---

## Version numbers (app releases)

Bump in `app/build.gradle.kts` when shipping a build to Play Console:

| Field | Rule |
|-------|------|
| `versionCode` | Integer — **must increase** every Play upload |
| `versionName` | Human-readable string (e.g. `1.0.1`) |

Commit version bumps on **`dev`**, then promote through **`uat`** → **`master`** with the same merge flow above.

Full release checklist: [release_and_versioning_guide.md §7](release_and_versioning_guide.md#7-citizen-app-release).

---

## Large files & push timeouts

Do **not** commit release binaries or calibration datasets (see `.gitignore`).

If large files were committed earlier, untrack once:

```powershell
git rm -r --cached app/release/
git rm -r --cached "tools/train_pothole_model/dataset/"
git add .gitignore
git commit -m "Stop tracking release binaries and calibration dataset"
git push origin dev
```

If `git push` fails with **HTTP 408**, increase buffer / use SSH — see troubleshooting in team notes or retry on stable Wi‑Fi.

---

## Git identity & remote

| Item | Value |
|------|--------|
| GitHub user | `citygridhelp` |
| Email | `citygridhelp@gmail.com` |
| Repository | `citygridhelp/citygrid_citizen` |

Verify:

```powershell
git config --global user.name
git config --global user.email
git remote -v
```

---

## Checklist

**Dev push**

- [ ] On branch `dev`
- [ ] `git pull origin dev`
- [ ] Commit with clear message
- [ ] `git push origin dev`
- [ ] Tested on device / emulator

**Dev → UAT**

- [ ] Dev verified
- [ ] Merged `dev` into `uat`
- [ ] `git push origin uat`
- [ ] UAT build tested

**UAT → Prod**

- [ ] UAT sign-off
- [ ] `versionCode` / `versionName` bumped if releasing to Play
- [ ] Merged `uat` into `master`
- [ ] `git push origin master`
- [ ] Play Console upload from production tag/branch as per [release guide](release_and_versioning_guide.md)
