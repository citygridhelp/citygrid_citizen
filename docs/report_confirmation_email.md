# Report submission confirmation email

Sends a **confirmation email** to signed-in citizens when a report row is inserted
into `public.reports`. Triggered by a **Database Webhook** → Edge Function (no
Android app changes).

Related: [supabase_email_templates.md](supabase_email_templates.md) (Auth OTP mail is separate).

---

## Confirmed product settings

| Item | Value |
|------|--------|
| Trigger | Database Webhook on `reports` **INSERT** |
| Ticket | `CG-` + last 8 digits of `reports.id` |
| User ID in email | `reporter_user_id` (`PW-xxx`) |
| Timestamp | **IST** (`Asia/Kolkata`) |
| Mail provider | **Brevo** (HTTP API from Edge Function) |
| Idempotency | `report_email_log` table (migration `0010`) |
| Guests | No email (`reporter_auth_id` null) |

---

## Email body (field order)

```text
City Grid — Report received

Ticket:     CG-23456789
User ID:    PW-A1B2C3D4
Location:   Koramangala 5th Block, Bengaluru (12.93524, 77.62448)
Severity:   MODERATE
Note:       ...
Submitted:  15 Jun 2026, 14:32 IST

You can track this report in the app under My Reports.
```

---

## Deploy checklist (dashboard only)

No Supabase CLI required. Everything below is done in the [Supabase dashboard](https://supabase.com/dashboard).

### 1. SQL migration (SQL Editor)

1. Open your project → **SQL Editor** → **New query**.
2. Copy the full contents of [`supabase/migrations/0010_report_email_log.sql`](../supabase/migrations/0010_report_email_log.sql) from this repo.
3. Paste → **Run**.
4. Confirm: **Table Editor** → `report_email_log` exists (may be empty).

### 2. Brevo (skip if you already have account + verified sender)

You only need your **API key value** (`xkeysib-...`) and the **verified sender email** for step 3. The label you gave the key in Brevo (e.g. `report_confirmation`) does not matter for Supabase.

### 3. Edge Function secrets (dashboard)

1. **Edge Functions** (left sidebar) → **Secrets** (or **Manage secrets**).
2. Add these **names** exactly (values are yours):

| Secret name | Value |
|-------------|--------|
| `BREVO_API_KEY` | Your Brevo v3 API key (`xkeysib-...`) |
| `MAIL_FROM` | Your verified Brevo sender email |
| `MAIL_FROM_NAME` | `City Grid` |

`SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are already available to Edge Functions — do not add them manually.

Optional later: `WEBHOOK_SECRET` if you use a custom HTTP webhook with a header check.

### 4. Create & deploy the Edge Function (dashboard editor)

1. **Edge Functions** → **Deploy a new function** (or **Create function**).
2. **Name:** `notify-citizen-report-created` (must match webhook step 5).
3. Open the in-browser code editor.
4. **Delete** the template code. Copy **all** of [`supabase/functions/notify-citizen-report-created/index.ts`](../supabase/functions/notify-citizen-report-created/index.ts) from this repo and paste it.
5. **Important:** Turn **off** “Verify JWT” / enable **anonymous invocations** for this function (wording varies). Database Webhooks call the function **without** a user login token.
6. **Deploy** / **Save**.

Check **Logs** under this function after testing.

### 5. Database Webhook

1. **Database** → **Webhooks** → **Create a new hook** (or **Enable Webhooks** first if prompted).
2. Configure:

| Field | Value |
|-------|--------|
| Name | `report_confirmation_email` |
| Schema | `public` |
| Table | `reports` |
| Events | **Insert** only (not Update / Delete) |
| Webhook type | **Supabase Edge Functions** |
| Edge Function | `notify-citizen-report-created` |
| HTTP method | POST |

3. **Create webhook**.

If your project only offers **HTTP Request** instead of Edge Function picker:

- URL: `https://YOUR_PROJECT_REF.supabase.co/functions/v1/notify-citizen-report-created`
- Method: POST  
- Replace `YOUR_PROJECT_REF` from **Project Settings** → **General** → **Reference ID**.

### 6. Test

1. Sign in on the citizen app and submit a pothole report.
2. Check the citizen’s inbox for `City Grid — Report CG-xxxxxxxx received`.
3. **SQL Editor:**
   ```sql
   select * from report_email_log order by sent_at desc limit 5;
   ```
4. **Edge Functions** → `notify-citizen-report-created` → **Logs** if nothing arrives.

---

## Deploy checklist (CLI — optional)

<details>
<summary>Click if you prefer terminal instead of dashboard</summary>

### Secrets

```powershell
cd c:\Users\priye\AndroidStudioProjects\potholereport
supabase login
supabase link --project-ref YOUR_PROJECT_REF
supabase secrets set BREVO_API_KEY="xkeysib-..."
supabase secrets set MAIL_FROM="noreply@yourdomain.com"
supabase secrets set MAIL_FROM_NAME="City Grid"
```

### Deploy function

```powershell
supabase functions deploy notify-citizen-report-created --no-verify-jwt
```

Webhook step is the same as dashboard §5 above.

</details>

---

## Idempotency (`report_email_log`)

| Column | Purpose |
|--------|---------|
| `report_id` | Primary key — one confirmation per report |
| `recipient` | Citizen email (after send) |
| `ticket` | `CG-xxxxxxxx` sent in the subject/body |
| `sent_at` | When the row was claimed/sent |

Flow:

1. **Insert** log row first (claim). Duplicate `report_id` → skip (webhook retry).
2. Look up email, send via Brevo.
3. On failure → **delete** log row so a retry can try again.
4. On success → **update** `recipient` from `pending`.

---

## Troubleshooting

| Symptom | Check |
|---------|--------|
| No email | Edge Function logs; Brevo sender verified; `reporter_auth_id` set on row |
| **Brevo API 401 + unrecognised IP address** | Brevo **Security → Authorized IPs** — disable IP lock or allow all IPs (see below) |
| 401 from function | `WEBHOOK_SECRET` header mismatch; or JWT verify still on |
| 502 Brevo error (other) | API key, sender domain, Brevo dashboard activity log |
| Guest report | Expected skip — no `reporter_auth_id` |

### Brevo 401: “unrecognised IP address”

Supabase Edge Functions call Brevo from **changing cloud IPs** (your log showed an IPv6 in
`ap-south-1`). If Brevo **Authorized IPs** is enabled, those requests are rejected.

**Fix (recommended for Edge Functions):**

1. Brevo → **Security** → **Authorized IPs**  
   [https://app.brevo.com/security/authorised_ips](https://app.brevo.com/security/authorised_ips)
2. **Disable** IP restriction for API/SMTP, **or** enable **allow all IPs** (wording varies).
3. Submit another signed-in report and check logs again.

Do **not** try to whitelist a single Supabase IP — serverless egress IPs are not fixed.

The **Node.js 20 deprecated** warning in function logs comes from `@supabase/supabase-js`
via npm in Deno. It does **not** block email; safe to ignore for now.

Function logs: Supabase dashboard → **Edge Functions** → `notify-citizen-report-created` → **Logs**.

---

## Out of scope (v1)

- Guest confirmation emails
- Status-change emails (OPEN → COMPLETED)
- Storing `CG-xxx` on `reports` (derived at send time only)

---

## Repo files

| Path | Role |
|------|------|
| `supabase/migrations/0010_report_email_log.sql` | Idempotency table |
| `supabase/functions/notify-citizen-report-created/index.ts` | Webhook handler + Brevo send |
