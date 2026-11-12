/**
 * Copy to supabase-config.js and fill from local.properties:
 *   SUPABASE_URL=…
 *   SUPABASE_ANON_KEY=…
 *
 * Or run:  python docs/web/scripts/gen_supabase_config.py
 *
 * The anon key is public (same as the Android app). Never put the service-role key here.
 * supabase-config.js is gitignored — add it on the machine / CI that deploys Pages.
 */
window.CG_SUPABASE = {
  url: "https://YOUR_PROJECT.supabase.co",
  anonKey: "YOUR_SUPABASE_ANON_KEY"
};
