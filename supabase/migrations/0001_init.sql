-- ============================================================================
-- Pothole two-way integration — initial schema
-- Citizen app  : com.example.potholereport
-- Government app: pothole.govt
--
-- Run this in the Supabase SQL Editor (or via `supabase db push`) on a fresh
-- project. It is idempotent-ish: it drops nothing, but uses IF NOT EXISTS where
-- Postgres allows so you can re-run safely after fixing errors.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 0. Extensions
-- ---------------------------------------------------------------------------
create extension if not exists "pgcrypto";   -- gen_random_uuid(), if needed later

-- ---------------------------------------------------------------------------
-- 1. Enums
--    Values match the Kotlin enums in BOTH apps exactly (enum .name strings).
-- ---------------------------------------------------------------------------
do $$ begin
  create type report_status as enum ('OPEN','IN_PROGRESS','VERIFICATION','COMPLETED');
exception when duplicate_object then null; end $$;

do $$ begin
  create type report_severity as enum ('MINOR','MODERATE','SEVERE','CRITICAL');
exception when duplicate_object then null; end $$;

do $$ begin
  create type gov_role as enum ('COMMISSIONER','ZONE_HEAD','FIELD_OFFICER');
exception when duplicate_object then null; end $$;

-- ---------------------------------------------------------------------------
-- 2. Government officers (fixed roster, mirrors GovAuthRepository seededAccounts)
--    One row per officer, linked to a Supabase Auth user.
--    Columns carry everything needed to rebuild GovOfficerSession on login.
-- ---------------------------------------------------------------------------
create table if not exists public.gov_officers (
  auth_id          uuid primary key references auth.users(id) on delete cascade,
  email            text unique not null,
  display_name     text not null,
  role             gov_role not null,
  city_key         text not null,
  position         text not null default '',
  corporation      text not null default '',
  zone_label       text not null default '',
  -- zone head: own zone key (e.g. BENGALURU:EAST)
  -- field officer: their zone head's key (their parent)
  assignee_key     text not null default '',
  -- field officer only (e.g. BENGALURU:EAST:ENG_01)
  field_officer_key text not null default '',
  created_at       timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- 3. Reports (canonical, citizen-created + govt-managed)
--    Column names map to the existing JSON contract used by both apps.
-- ---------------------------------------------------------------------------
create table if not exists public.reports (
  id                bigint primary key,             -- citizen timestamp id (createdAtMs)
  city_key          text not null,
  created_at_ms     bigint not null,
  reporter_user_id  text not null default '',       -- privacy-safe key (PW-/PD-)
  reporter_auth_id  uuid references auth.users(id), -- set when reporter is signed in

  -- evidence (Supabase Storage object paths, not device paths)
  photo_path        text default '',                -- close-up
  wide_photo_path   text default '',
  latitude          double precision,
  longitude         double precision,
  area_label        text not null default '',
  severity          report_severity not null default 'MODERATE',
  citizen_note      text not null default '',

  -- lifecycle
  status                  report_status not null default 'OPEN',  -- internal (govt) status
  citizen_visible_status  report_status not null default 'OPEN',  -- VERIFICATION/pre-approval COMPLETED -> IN_PROGRESS

  -- assignment (current zone head / owner)
  assignee_key      text not null default '',
  assignee_corp     text not null default '',
  assignee_zone     text not null default '',
  assignee_name     text not null default '',
  assignee_role     text not null default '',
  assignee_addr     text not null default '',

  -- field officer delegation
  field_officer_key  text not null default '',
  field_officer_name text not null default '',
  field_officer_role text not null default '',

  -- completion publication (govt -> citizen, after commissioner approval)
  completion_top_path           text default '',
  completion_wide_path          text default '',
  completed_at_ms               bigint,
  published_by_key              text default '',
  published_by_name             text default '',
  commissioner_completion_note  text not null default '',

  updated_at        timestamptz not null default now()
);

create index if not exists idx_reports_city           on public.reports(city_key);
create index if not exists idx_reports_status          on public.reports(status);
create index if not exists idx_reports_assignee        on public.reports(assignee_key);
create index if not exists idx_reports_field_officer   on public.reports(field_officer_key);
create index if not exists idx_reports_reporter_auth   on public.reports(reporter_auth_id);

-- ---------------------------------------------------------------------------
-- 4. Status audit trail (mirrors StatusChangeRecord in the govt app)
-- ---------------------------------------------------------------------------
create table if not exists public.status_history (
  id              bigserial primary key,
  report_id       bigint not null references public.reports(id) on delete cascade,
  from_status     report_status,
  to_status       report_status not null,
  reason          text not null default '',
  changed_at_ms   bigint not null,
  changed_by_key  text not null default '',
  changed_by_name text not null default '',
  changed_by_role text not null default ''
);

create index if not exists idx_status_history_report on public.status_history(report_id);

-- ---------------------------------------------------------------------------
-- 5. Proof attachments (completion TOP/SIDE/WIDE + decline proofs)
--    Stored as Storage object paths.
-- ---------------------------------------------------------------------------
create table if not exists public.report_proofs (
  id            bigserial primary key,
  report_id     bigint not null references public.reports(id) on delete cascade,
  kind          text not null,   -- COMPLETION_TOP | COMPLETION_SIDE | COMPLETION_WIDE | DECLINE
  storage_path  text not null,
  uploaded_by_key text not null default '',
  created_at_ms bigint not null
);

create index if not exists idx_report_proofs_report on public.report_proofs(report_id);

-- ---------------------------------------------------------------------------
-- 6. updated_at trigger
-- ---------------------------------------------------------------------------
create or replace function public.touch_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists trg_reports_touch on public.reports;
create trigger trg_reports_touch
  before update on public.reports
  for each row execute function public.touch_updated_at();

-- ---------------------------------------------------------------------------
-- 7. Helper: caller's officer row (NULL for citizens)
--    SECURITY DEFINER so RLS policies can call it without recursive RLS.
-- ---------------------------------------------------------------------------
create or replace function public.current_officer()
returns public.gov_officers
language sql
stable
security definer
set search_path = public
as $$
  select * from public.gov_officers where auth_id = auth.uid();
$$;

create or replace function public.is_officer()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (select 1 from public.gov_officers where auth_id = auth.uid());
$$;

-- ---------------------------------------------------------------------------
-- 8. Row Level Security
-- ---------------------------------------------------------------------------
alter table public.gov_officers   enable row level security;
alter table public.reports        enable row level security;
alter table public.status_history enable row level security;
alter table public.report_proofs  enable row level security;

-- --- gov_officers: an officer can read their own row (login -> session) -----
drop policy if exists officer_read_self on public.gov_officers;
create policy officer_read_self on public.gov_officers
  for select to authenticated
  using (auth_id = auth.uid());

-- --- reports: citizen inserts their own --------------------------------------
drop policy if exists citizen_insert_report on public.reports;
create policy citizen_insert_report on public.reports
  for insert to authenticated
  with check (reporter_auth_id = auth.uid() and not public.is_officer());

-- --- reports: read ----------------------------------------------------------
-- Citizen sees their own; officers see scoped rows in their city.
drop policy if exists read_reports on public.reports;
create policy read_reports on public.reports
  for select to authenticated
  using (
    reporter_auth_id = auth.uid()
    or exists (
      select 1 from public.current_officer() o
      where o.city_key = reports.city_key
        and (
          o.role = 'COMMISSIONER'
          or (o.role = 'ZONE_HEAD'
              and (reports.assignee_key = o.assignee_key
                   or reports.field_officer_key like o.assignee_key || ':%'))
          or (o.role = 'FIELD_OFFICER'
              and reports.field_officer_key = o.field_officer_key)
        )
    )
  );

-- --- reports: officers update workflow fields in their city ------------------
-- (Column-level guarding of citizen evidence is enforced via the app + a
--  future SECURITY DEFINER RPC; for the prototype officers in-city may update.)
drop policy if exists officer_update_report on public.reports;
create policy officer_update_report on public.reports
  for update to authenticated
  using (
    exists (select 1 from public.current_officer() o where o.city_key = reports.city_key)
  )
  with check (
    exists (select 1 from public.current_officer() o where o.city_key = reports.city_key)
  );

-- --- status_history: officers in city write; everyone with report read sees --
drop policy if exists read_status_history on public.status_history;
create policy read_status_history on public.status_history
  for select to authenticated
  using (
    exists (select 1 from public.reports r where r.id = status_history.report_id) -- visibility enforced by reports RLS via join in app
  );

drop policy if exists officer_insert_status_history on public.status_history;
create policy officer_insert_status_history on public.status_history
  for insert to authenticated
  with check (public.is_officer());

-- --- report_proofs: officers in city write; report viewers read --------------
drop policy if exists read_report_proofs on public.report_proofs;
create policy read_report_proofs on public.report_proofs
  for select to authenticated
  using (true);  -- prototype: paths are unguessable; tighten with report join if needed

drop policy if exists officer_write_report_proofs on public.report_proofs;
create policy officer_write_report_proofs on public.report_proofs
  for insert to authenticated
  with check (public.is_officer());

-- ---------------------------------------------------------------------------
-- 9. Storage buckets + policies
--    Run AFTER creating the buckets in the dashboard, OR rely on these inserts.
-- ---------------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('evidence','evidence', false)
on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
values ('proofs','proofs', false)
on conflict (id) do nothing;

-- Authenticated users may upload citizen evidence; read is broad for the
-- prototype (object keys are report-id based and unguessable). Tighten later.
drop policy if exists evidence_read on storage.objects;
create policy evidence_read on storage.objects
  for select to authenticated
  using (bucket_id = 'evidence');

drop policy if exists evidence_insert on storage.objects;
create policy evidence_insert on storage.objects
  for insert to authenticated
  with check (bucket_id = 'evidence');

drop policy if exists proofs_read on storage.objects;
create policy proofs_read on storage.objects
  for select to authenticated
  using (bucket_id = 'proofs');

-- Only officers upload repair/decline proofs.
drop policy if exists proofs_insert on storage.objects;
create policy proofs_insert on storage.objects
  for insert to authenticated
  with check (bucket_id = 'proofs' and public.is_officer());

-- ---------------------------------------------------------------------------
-- 10. Realtime: broadcast row changes to subscribed clients
-- ---------------------------------------------------------------------------
alter publication supabase_realtime add table public.reports;
alter publication supabase_realtime add table public.status_history;
alter publication supabase_realtime add table public.report_proofs;
