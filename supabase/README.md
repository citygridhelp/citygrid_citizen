# Supabase backend for the Pothole apps

Shared backend that connects the citizen app (`com.example.potholereport`) and
the government app (`pothole.govt`) two-way.

```
supabase/
  migrations/
    0001_init.sql        # schema: enums, reports, status_history, report_proofs,
                         # gov_officers, RLS policies, storage buckets, realtime
    0002_email_exists.sql
    0003_officer_workflow_sync.sql  # GRANTs + officer_sync_report_workflow RPC
    0004_status_history_rpc.sql     # status_history insert inside RPC (required)
    0005_officer_completion.sql     # completion proofs + commissioner publish RPC
  seed/
    officers.json        # the 14-officer roster (mirrors GovAuthRepository)
    seed_officers.mjs    # admin script: creates Auth users + gov_officers rows
    package.json
```

## 1. Apply the schema

Supabase dashboard → **SQL Editor** → paste the contents of
`migrations/0001_init.sql` → **Run**.

(Or with the Supabase CLI: put the file under `supabase/migrations/` and run
`supabase db push`.)

This creates the tables, enums, RLS policies, the `evidence` and `proofs`
Storage buckets, and adds the tables to the realtime publication.

Then run `migrations/0002_email_exists.sql` (citizen login errors) and
**`migrations/0003_officer_workflow_sync.sql`** (required for government app
status/assignee write-back to `reports`) and
**`migrations/0004_status_history_rpc.sql`** (audit trail in `status_history`) and
**`migrations/0005_officer_completion.sql`** (repair proof upload + commissioner publish).

## 2. Seed government officers

The officers are a fixed roster, so create them with the admin script (it uses
the **service_role** key — run locally, never inside the apps).

```powershell
cd supabase/seed
npm install

$env:SUPABASE_URL="https://YOUR-PROJECT.supabase.co"
$env:SUPABASE_SERVICE_ROLE_KEY="<service_role key from Project Settings → API>"
$env:OFFICER_PASSWORD="gov123"   # optional; matches the current demo password

node seed_officers.mjs
```

You should see 14 officers created (2 commissioners, 7 zone heads, 5 field
officers). Re-running is safe: it updates existing users and resets passwords.

## 3. Wire the apps

Use the **Project URL** and the **anon public** key (NOT service_role) in both
Android apps via `BuildConfig`/`local.properties`. See the integration plan for
the per-app code steps (SDK setup, insert on report submit, status updates,
realtime subscriptions).

## 4. Email templates (OTP)

Signup and profile email change expect **numeric codes** in email, not links.
Configure **Authentication → Email Templates** so **Confirm signup** and
**Change email address** include `{{ .Token }}`.

**Secure email change** should be **off** (Authentication → Sign In / Providers →
Email) so profile email change uses a **single code** to the new address. Details:
[`docs/supabase_email_templates.md`](../docs/supabase_email_templates.md).

## Field mapping (single source of truth)

| Citizen JSON | `reports` column | Govt JSON |
|---|---|---|
| `id` | `id` | `id` |
| `city` | `city_key` | `cityKey` |
| `path` | `photo_path` | `photoPath` |
| `widePath` | `wide_photo_path` | `widePhotoPath` |
| `severity` | `severity` | `severity` |
| `note` / `citizenNote` | `citizen_note` | `citizenNote` |
| `status` | `status` / `citizen_visible_status` | `status` |
| `assigneeKey` | `assignee_key` | `assigneeKey` |
| `reporterId` | `reporter_user_id` | `reporterUserId` |
| `completionProofTopPath` | `completion_top_path` | completion TOP |
| `commissionerCompletionNote` | `commissioner_completion_note` | `commissionerCompletionNote` |
