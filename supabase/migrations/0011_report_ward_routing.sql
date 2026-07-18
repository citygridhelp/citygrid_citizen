-- GBA ward routing metadata on citizen reports (reference routing at submit time).
alter table public.reports
  add column if not exists ward_key text not null default '',
  add column if not exists ward_number int not null default 0,
  add column if not exists ward_name text not null default '';

comment on column public.reports.ward_key is
  'Official GBA ward assignee key, e.g. BENGALURU:GBA_EAST:W046 (citizen routing snapshot).';
comment on column public.reports.ward_number is 'GBA ward number within corporation (2025 delimitation).';
comment on column public.reports.ward_name is 'Official GBA ward name at submit time.';

create index if not exists idx_reports_ward_key on public.reports (ward_key)
  where ward_key <> '';
