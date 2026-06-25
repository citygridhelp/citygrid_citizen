#!/usr/bin/env node
/**
 * Officer-seeding admin script.
 *
 * Creates one Supabase Auth user per government officer (from officers.json)
 * and upserts a matching row into public.gov_officers so the govt app can
 * rebuild GovOfficerSession after login.
 *
 * USES THE SERVICE_ROLE KEY — run this ONLY from a trusted machine / CI.
 * Never ship the service_role key inside either Android app.
 *
 * Usage (PowerShell):
 *   cd supabase/seed
 *   npm install
 *   $env:SUPABASE_URL="https://YOUR-PROJECT.supabase.co"
 *   $env:SUPABASE_SERVICE_ROLE_KEY="eyJ...service_role..."
 *   $env:OFFICER_PASSWORD="gov123"        # optional, defaults to gov123
 *   node seed_officers.mjs
 *
 * Re-running is safe: existing users are looked up and their gov_officers row
 * is upserted (and password reset to OFFICER_PASSWORD).
 */

import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { createClient } from '@supabase/supabase-js';

const __dirname = dirname(fileURLToPath(import.meta.url));

const SUPABASE_URL = process.env.SUPABASE_URL;
const SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;
const OFFICER_PASSWORD = process.env.OFFICER_PASSWORD || 'gov123';

if (!SUPABASE_URL || !SERVICE_ROLE_KEY) {
  console.error(
    'Missing env vars. Set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY before running.',
  );
  process.exit(1);
}

const admin = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
  auth: { autoRefreshToken: false, persistSession: false },
});

/** Find an existing auth user by email (paginates the admin list API). */
async function findUserByEmail(email) {
  const target = email.toLowerCase();
  let page = 1;
  const perPage = 200;
  // eslint-disable-next-line no-constant-condition
  while (true) {
    const { data, error } = await admin.auth.admin.listUsers({ page, perPage });
    if (error) throw error;
    const match = data.users.find((u) => (u.email || '').toLowerCase() === target);
    if (match) return match;
    if (data.users.length < perPage) return null;
    page += 1;
  }
}

/** Create the auth user if absent, else reset its password. Returns the user id. */
async function ensureAuthUser(email) {
  const existing = await findUserByEmail(email);
  if (existing) {
    const { error } = await admin.auth.admin.updateUserById(existing.id, {
      password: OFFICER_PASSWORD,
      email_confirm: true,
    });
    if (error) throw error;
    return { id: existing.id, created: false };
  }
  const { data, error } = await admin.auth.admin.createUser({
    email,
    password: OFFICER_PASSWORD,
    email_confirm: true,
  });
  if (error) throw error;
  return { id: data.user.id, created: true };
}

async function main() {
  const raw = await readFile(join(__dirname, 'officers.json'), 'utf8');
  const officers = JSON.parse(raw);

  console.log(`Seeding ${officers.length} officers into ${SUPABASE_URL}\n`);

  let created = 0;
  let updated = 0;

  for (const o of officers) {
    const email = o.email.toLowerCase();
    try {
      const { id: authId, created: wasCreated } = await ensureAuthUser(email);

      const { error: upsertErr } = await admin.from('gov_officers').upsert(
        {
          auth_id: authId,
          email,
          display_name: o.displayName,
          role: o.role,
          city_key: o.cityKey,
          position: o.position ?? '',
          corporation: o.corporation ?? '',
          zone_label: o.zoneLabel ?? '',
          assignee_key: o.assigneeKey ?? '',
          field_officer_key: o.fieldOfficerKey ?? '',
        },
        { onConflict: 'auth_id' },
      );
      if (upsertErr) throw upsertErr;

      if (wasCreated) created += 1; else updated += 1;
      console.log(`  ${wasCreated ? 'created' : 'updated'}  ${o.role.padEnd(13)} ${email}`);
    } catch (err) {
      console.error(`  FAILED   ${email}: ${err.message || err}`);
    }
  }

  console.log(`\nDone. ${created} created, ${updated} updated.`);
  console.log(`All officers share password: ${OFFICER_PASSWORD}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
