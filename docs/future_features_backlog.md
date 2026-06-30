# Citizen app — future features backlog

Planned or deferred work for the City Grid citizen app (`potholereport`). Items here are **not** in the current release scope unless marked as shipped.

For release steps and what is already live, see [release_and_versioning_guide.md](release_and_versioning_guide.md).

---

## Auth & citizen identity

### 1. Separate account per email (current design — no app work)

**Status:** Shipped by default (Supabase Auth behaviour).

Each successful signup with a **new, unused** email creates:

- a new row in `auth.users` (`auth_id`)
- a new `citizen_profiles` row and `PW-xxx` reporter id
- a separate login and separate “My Reports” scope

There is **no account linking**. A person who signs up with `a@gmail.com` and later with `b@gmail.com` (if both are unused) has **two independent accounts**.

**Duplicate email blocked:** signup calls the `email_exists` RPC (`0002_email_exists.sql`) before `signUpWith`. Existing Auth emails show: *“An account with this email already exists. Sign in instead.”*

---

### 2. Account linking

**Status:** Not implemented — future feature.

**Goal:** Allow one real person to tie **multiple email addresses** to a **single** citizen identity (`auth_id`, one `PW-xxx`, one merged “My Reports” history).

**Why deferred:** Current product uses one email = one account. Linking needs explicit UX (verify both emails, merge reports, handle conflicts) and server rules (which `auth_id` survives, what happens to the second account’s reports).

**Rough scope when picked up:**

- [ ] Design: primary account + add/verify secondary email, or merge two existing accounts
- [ ] Supabase: schema/RPC for linked identities or single auth user with multiple verified emails
- [ ] App: settings flow + merge `citizen_profiles` / reassign `reports.reporter_auth_id` where needed
- [ ] Prevent abuse: rate limits, both-inbox verification, audit log

---

### 3. Email change history (audit trail)

**Status:** Not implemented — optional future.

**Current behaviour:** Only the **current** email is stored in `auth.users` and mirrored to `citizen_profiles.email` (migration `0009_citizen_profile_email_sync.sql`). Old addresses are not kept in `citizen_profiles`.

**If needed later:** add e.g. `citizen_email_history (auth_id, email, changed_at)` for support/audit — not required for duplicate signup prevention (`email_exists` uses `auth.users` only).

---

## Reports & sync

### 4. Download report photos from Supabase on sync

**Status:** Shipped (report photo cache + sync in citizen app).

Signed-in and public feed sync download evidence photos into app storage via
`ReportPhotoCache`. See `ReportSyncRepository` / `RecentReportsRepository`.

---

### 5. Stricter city / metro match on report submit

**Status:** Partially shelved (was in IDE shelved patches).

Validate that report GPS / picked map location lies inside the selected city metro before submit, with clear citizen-facing errors.

---

## UI & notifications

### 6. In-app notifications bell

**Status:** Not implemented (shelved patch).

Header bell for citizen notifications (e.g. app update prompts, report status messages) wired to `CitizenNotificationsRepository`.

---

### 7. Email verification settings tab (admin / template polish)

**Status:** Partially deferred.

Signup and profile email change rely on Supabase email templates using `{{ .Token }}` (Confirm signup, Change email address) and **Secure email change off** for a single-code profile update. Operator checklist: [`docs/supabase_email_templates.md`](supabase_email_templates.md).

---

### 8. Report submission confirmation email

**Status:** Implemented in repo — **deploy** migration `0010`, Brevo secrets, Edge Function, and Database Webhook.

After a **signed-in** citizen’s report is inserted into `public.reports`, a webhook
calls `notify-citizen-report-created` to email the citizen via Brevo.

| Item | Decision |
|------|----------|
| Ticket in email | `CG-` + last 8 digits of `reports.id` |
| User ID in email | Raw `reporter_user_id` (`PW-xxx`) |
| Timestamp | IST |
| Idempotency | `report_email_log` |

Deploy guide: [`docs/report_confirmation_email.md`](report_confirmation_email.md).

---

## Government / cross-app

### 9. Deeper two-way sync with CG GOVT app

**Status:** Ongoing / handover.

Citizen push to `reports` + `evidence` is implemented. Full round-trip (all status transitions, proof photos visible in citizen My Reports) depends on government app workflows and migrations — see [government_app_handover.md](government_app_handover.md).

---

## How to use this list

| Priority | Suggested order |
|----------|-----------------|
| High (product) | Deploy #8 confirmation email (Brevo + webhook), #5 city match |
| Medium | #6 notifications bell |
| Low / strategic | #2 account linking, #3 email history |
| Reference only | #1 (document current design) |
| Shipped | #4 photo download |

When an item ships, move it to the release guide or handover doc and mark it **Shipped** here with the migration/app version.
