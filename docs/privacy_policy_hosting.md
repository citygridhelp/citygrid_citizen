# Privacy policy — GitHub Pages & Play Console

How to publish [`privacy_policy.md`](privacy_policy.md) on GitHub and link it in Google Play.

---

## Step 1 — Commit the policy to your repo

The policy file is already in this repo:

```text
docs/privacy_policy.md
```

Push to GitHub (`citygridhelp/citygrid_citizen`):

```powershell
cd c:\Users\priye\AndroidStudioProjects\potholereport
git add docs/privacy_policy.md docs/privacy_policy_hosting.md
git commit -m "Add privacy policy for Play Store"
git push origin dev
```

Merge to `uat` / `master` when ready (see [git_branching_versioning.md](git_branching_versioning.md)).

---

## Step 2 — Enable GitHub Pages

1. Open **https://github.com/citygridhelp/citygrid_citizen**
2. **Settings** → **Pages** (left sidebar)
3. **Build and deployment**
   - **Source:** Deploy from a branch
   - **Branch:** `master` (or `main`) → folder **`/docs`** → **Save**
4. Wait 1–3 minutes. GitHub shows your site URL, e.g.:

```text
https://citygridhelp.github.io/citygrid_citizen/
```

Your privacy policy URL will be:

```text
https://citygridhelp.github.io/citygrid_citizen/privacy_policy
```

Open that link in an **incognito** window — it must load **without login**.

> **Tip:** If the page 404s, ensure `privacy_policy.md` is on the branch you selected for Pages (usually `master`), not only on `dev`.

---

## Step 3 — Add URL in Google Play Console

1. [Google Play Console](https://play.google.com/console) → your app **City Grid**
2. **Policy** → **App content** (or **Store settings**)
3. **Privacy policy** → paste:

```text
https://citygridhelp.github.io/citygrid_citizen/privacy_policy
```

4. **Save**

Use the same URL in **Data safety** if asked for a policy link.

**Delete account URL** (Data safety → Data collection and security):

```text
https://citygridhelp.github.io/citygrid_citizen/privacy_policy#7-account-deletion
```

This anchor opens **Section 7 — Account deletion** on the same page. Verify it in incognito after GitHub Pages is live.

---

## Step 4 — Align Data safety (must match the policy)

In Play Console → **App content** → **Data safety**, declare what the app actually collects:

| Data type | Collected? | Purpose | Shared? |
|-----------|------------|---------|---------|
| **Email address** | Yes | Account management | Yes (Supabase, Brevo) |
| **Name** | Yes | Account (signup) | Yes (Supabase) |
| **Precise location** | Yes | App functionality (reports, map) | Yes (Supabase) |
| **Photos** | Yes | App functionality (evidence) | Yes (Supabase) |
| **App interactions** | Optional / No | — | — |
| **Crash logs** | No (unless you add Firebase later) | — | — |
| **Advertising** | No | — | — |
| **Data sold** | No | — | — |
| **Encrypted in transit** | Yes | HTTPS | — |
| **Deletion request** | Yes | Email citygridhelp@gmail.com | — |

**Permissions in app:** Location, Camera, Internet (see `AndroidManifest.xml`).

Policy and Data safety **must agree** — mismatches are a common rejection reason.

---

## Optional — shorter public URL

If you later buy a domain (e.g. `citygrid.in`):

```text
https://citygrid.in/privacy
```

Update Play Console when the new URL is live.

---

## Checklist

- [ ] `docs/privacy_policy.md` pushed to the branch used for GitHub Pages
- [ ] GitHub Pages enabled (`/docs` on `master`)
- [ ] Policy URL opens in incognito
- [ ] URL added in Play Console → Privacy policy
- [ ] Data safety form completed and consistent with policy
- [ ] Delete account URL added in Data safety (`.../privacy_policy#7-account-deletion`)
