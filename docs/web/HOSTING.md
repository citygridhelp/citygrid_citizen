# City Grid microsite — hosting

Static pages live in this folder:

| Path | URL (after Pages deploy) |
|------|---------------------------|
| `index.html` | `…/web/` |
| `c/index.html` | `…/web/c/` ← citizen card QR |
| `b/index.html` | `…/web/b/` ← commercial card QR |
| `start/index.html` | `…/web/start/` ← Get started |
| `auth/index.html` | `…/web/auth/` ← signup / login / OTP |
| `citizen/reports/` | `…/web/citizen/reports/` ← My reports (signed in) |
| `commercial/` | `…/web/commercial/` ← commercial after login |
| `privacy/index.html` | `…/web/privacy/` ← styled Privacy Policy |
| `login/`, `signup/` | Redirect to `auth/?role=citizen` |

Markdown source of truth for Play Console: `docs/privacy_policy.md` → `…/privacy_policy`.

## Auth (same Supabase as the Android app)

One account: email + password, signup confirms with a 6-digit email OTP (same flow as the app).

| Role | After login |
|------|-------------|
| Citizen (`?role=citizen`) | `citizen/reports/` — rows where `reporter_auth_id = auth.uid()` |
| Commercial (`?role=commercial`) | `commercial/` — account ready; fleet access still provisioned |

Config (anon key is public; never put the service-role key here):

1. Copy `assets/supabase-config.example.js` → `assets/supabase-config.js`, or run:
   ```powershell
   python docs\web\scripts\gen_supabase_config.py
   ```
   (reads `SUPABASE_URL` / `SUPABASE_ANON_KEY` from repo-root `local.properties`)
2. `supabase-config.js` is **gitignored** — generate it on the machine (or CI step) that deploys Pages.
3. In Supabase Dashboard → **Authentication → URL configuration**, add your site origins, e.g.:
   - `http://localhost:8080`
   - `https://citygridhelp.github.io`
   Site URL can stay the app deep-link if you prefer; OTP signup does not need a magic-link redirect.

Serve over **http(s)** — `file://` will not load the config reliably and blocks Auth storage.

## Social icons (footer)

Edit `assets/social.js` and paste real URLs:

```js
linkedin: "https://www.linkedin.com/in/…",
x: "https://x.com/…",
youtube: "https://www.youtube.com/@…",
instagram: "https://www.instagram.com/…"
```

Empty strings hide that icon (no dead links).

## Local preview

From repo root:

```powershell
python docs\web\scripts\gen_supabase_config.py
cd docs\web
python -m http.server 8080
```

Open http://localhost:8080/auth/index.html?role=citizen  
Prefer this over `file://` — Auth needs http(s).

## Do you need to spend money?

| Piece | Cost |
|-------|------|
| **Hosting** (GitHub Pages / Cloudflare Pages / Netlify) | **Free** for this tiny static site |
| **Custom domain** (e.g. `citygrid.in`) | **Yes** — usually ~₹500–1,500/year at a registrar |
| **SSL** | Free on the hosts above |

You already publish privacy from this repo on GitHub Pages  
(`https://citygridhelp.github.io/citygrid_citizen/`).  
Same free plan can serve `/web/`, `/web/c/`, `/web/b/`, `/web/auth/`.

**Recommendation**

1. **Now (₹0):** push this folder and use  
   `https://citygridhelp.github.io/citygrid_citizen/web/c/` and `…/web/b/`  
   for visiting-card back QRs (or a free short redirect into those URLs).  
   Deploy `supabase-config.js` alongside (generate in CI or copy before push if you choose to force-add once — anon only).
2. **When ready:** buy `citygrid.in` (or similar), point DNS at Pages, then map  
   `citygrid.in/c` → citizen and `citygrid.in/b` → business.

## Deploy (GitHub Pages — same as privacy)

1. Commit `docs/web/**` (except gitignored `supabase-config.js`) and push.
2. On the deploy machine: generate `supabase-config.js`, then include it in the published tree.
3. Repo **Settings → Pages**: source = branch, folder **`/docs`**.
4. Wait 1–3 minutes, then open:
   - https://citygridhelp.github.io/citygrid_citizen/web/
   - https://citygridhelp.github.io/citygrid_citizen/web/auth/?role=citizen
   - https://citygridhelp.github.io/citygrid_citizen/web/c/

## Optional: Cloudflare Pages (also free)

Connect the GitHub repo, set **root directory** to `docs/web`, deploy.  
Add a build step that writes `assets/supabase-config.js` from secrets (`SUPABASE_URL`, `SUPABASE_ANON_KEY`).

## Card QR targets

Keep card backs pointing at `/web/c/` and `/web/b/`; those pages now deep-link into web auth.

Homepage QR (site root): `assets/qr_homepage.png` →  
`https://citygridhelp.github.io/citygrid_citizen/web/`  
(also copied under `docs/pitch/qr_homepage.png` for print).
