-- Officer workflow write-back: grants + SECURITY DEFINER RPC.
-- Run in Supabase SQL Editor if reports.status / assignee_* stay unchanged
-- while status_history rows appear (UPDATE was silently affecting 0 rows).

-- ---------------------------------------------------------------------------
-- 1. Table grants (0001 defined RLS policies but not always table privileges)
-- ---------------------------------------------------------------------------
grant select, insert, update on public.reports to authenticated;
grant select, insert on public.status_history to authenticated;
grant select on public.gov_officers to authenticated;

-- ---------------------------------------------------------------------------
-- 2. RPC: officer workflow sync (bypasses UPDATE+SELECT RLS interaction issues)
-- ---------------------------------------------------------------------------
create or replace function public.officer_sync_report_workflow(
  p_report_id bigint,
  p_status text,
  p_citizen_visible_status text,
  p_assignee_key text default '',
  p_assignee_corp text default '',
  p_assignee_zone text default '',
  p_assignee_name text default '',
  p_assignee_role text default '',
  p_assignee_addr text default '',
  p_field_officer_key text default '',
  p_field_officer_name text default '',
  p_field_officer_role text default ''
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  o public.gov_officers;
  rows_updated int;
begin
  select * into o from public.gov_officers where auth_id = auth.uid();
  if o.auth_id is null then
    raise exception 'caller is not a registered officer';
  end if;

  update public.reports r
  set
    status = p_status::report_status,
    citizen_visible_status = p_citizen_visible_status::report_status,
    assignee_key = p_assignee_key,
    assignee_corp = p_assignee_corp,
    assignee_zone = p_assignee_zone,
    assignee_name = p_assignee_name,
    assignee_role = p_assignee_role,
    assignee_addr = p_assignee_addr,
    field_officer_key = p_field_officer_key,
    field_officer_name = p_field_officer_name,
    field_officer_role = p_field_officer_role,
    updated_at = now()
  where r.id = p_report_id
    and r.city_key = o.city_key;

  get diagnostics rows_updated = row_count;
  return rows_updated > 0;
end;
$$;

grant execute on function public.officer_sync_report_workflow(
  bigint, text, text, text, text, text, text, text, text, text, text, text
) to authenticated;

-- Tie status_history inserts to reports the officer's city owns (optional hardening)
drop policy if exists officer_insert_status_history on public.status_history;
create policy officer_insert_status_history on public.status_history
  for insert to authenticated
  with check (
    exists (
      select 1
      from public.reports r
      join public.gov_officers o on o.auth_id = auth.uid()
      where r.id = status_history.report_id
        and r.city_key = o.city_key
    )
  );
