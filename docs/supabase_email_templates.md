# Supabase email templates (OTP codes)

The citizen app verifies signup and profile email changes with **numeric OTP codes**
entered in the app — not confirmation links.

If users receive a **“Confirm your email” link** instead of a code, update the
matching template in the Supabase dashboard.

## Operator settings (City Grid default)

These Auth settings match how the app is designed and tested:

| Setting | Location | City Grid value |
|---------|----------|-----------------|
| **Secure email change** | Authentication → Sign In / Providers → **Email** | **Off** |
| Change email template | Authentication → **Email Templates** → Change email address | Body includes `{{ .Token }}` |
| Confirm signup template | Authentication → **Email Templates** → Confirm signup | Body includes `{{ .Token }}` |

With **Secure email change off**, profile email change needs **one code only** —
sent to the **new** address after the user taps **Send verification code**.

Direct link to Email provider (replace `YOUR_PROJECT_REF`):

`https://supabase.com/dashboard/project/YOUR_PROJECT_REF/auth/providers?provider=Email`

## Where to edit templates

Supabase dashboard → **Authentication** → **Email Templates**

## Required templates

### Confirm signup

Used when a new citizen registers. Body must include the token:

```text
Your City Grid verification code is: {{ .Token }}
```

Do **not** rely on `{{ .ConfirmationURL }}` alone for signup — the app calls
`verifyEmailOtp` with the code the user types.

### Change email address

Used when a signed-in citizen changes their profile email (single-code flow). Same
requirement:

```text
Your City Grid email change code is: {{ .Token }}
```

Flow:

1. User enters new email in Profile → Edit → **Send verification code**
2. Supabase sends **one** OTP to the **new** inbox (via `updateUser { email = … }`)
3. User enters the code → **Confirm verification code**
4. App verifies with `verifyEmailOtp(EMAIL_CHANGE, …)` and reloads the user from
   Supabase; `auth.users.email` and `citizen_profiles.email` update on success

Do **not** call `resendEmail` immediately after `updateUser` in the app — a second
send can invalidate the first code the user receives.

If this template still uses `{{ .ConfirmationURL }}`, users will only get a link
and the in-app “Verification code” field will never work.

## Profile email change (what users see)

1. Edit profile → new email → **Send verification code**
2. Check the **new** inbox for the numeric code (not the link)
3. Enter code → **Confirm verification code**
4. Profile returns to normal view with the updated masked email

No code is sent to the **old** email when Secure email change is off.

## Password recovery

Password reset uses Supabase’s **link-based** flow (`resetPasswordForEmail`). That
template should keep `{{ .ConfirmationURL }}` — no change needed for reset.

## Quick checklist

| Template              | Use in app        | Must include   |
|-----------------------|-------------------|----------------|
| Confirm signup        | Signup OTP        | `{{ .Token }}` |
| Change email address  | Profile email OTP | `{{ .Token }}` |
| Reset password        | External link     | `{{ .ConfirmationURL }}` |

After saving template or provider changes, trigger a fresh signup or email change
to confirm the email shows a numeric code and one confirm step completes the update.

## Optional: Secure email change on (not used for City Grid)

If you turn **Secure email change** **on** under Authentication → Sign In /
Providers → Email, Supabase sends a **second** OTP to the **current** email. The
app still supports that with a “Code from current email” step, but this is **not**
the configured City Grid flow. Prefer leaving the toggle **off** unless you
explicitly want two-step confirmation for higher-risk deployments.
