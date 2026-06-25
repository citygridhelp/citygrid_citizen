-- Fix status_history audit trail: insert inside the SECURITY DEFINER RPC so it
-- is not blocked by the tightened RLS policy added in 0003.
--
-- Root cause: 0003 changed status_history INSERT to require reading reports
-- under RLS in a WITH CHECK subquery. That subquery often fails even after a
-- successful workflow RPC update, so history inserts were silently rejected.

-- Drop the old 12-parameter signature before replacing.
drop function if exists public.officer_sync_report_workflow(
  bigint, text, text, text, text, text, text, text, text, text, text, text
);

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
  p_field_officer_role text default '',
  -- optional status_history row (pass p_history_to to append audit entry)
  p_history_from text default null,
  p_history_to text default null,
  p_history_reason text default '',
  p_history_at_ms bigint default null,
  p_history_by_key text default '',
  p_history_by_name text default '',
  p_history_by_role text default ''
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
  if rows_updated = 0 then
    return false;
  end if;

  if p_history_to is not null then
    insert into public.status_history (
      report_id,
      from_status,
      to_status,
      reason,
      changed_at_ms,
      changed_by_key,
      changed_by_name,
      changed_by_role
    ) values (
      p_report_id,
      p_history_from::report_status,
      p_history_to::report_status,
      coalesce(p_history_reason, ''),
      coalesce(p_history_at_ms, (extract(epoch from now()) * 1000)::bigint),
      coalesce(p_history_by_key, ''),
      coalesce(p_history_by_name, ''),
      coalesce(p_history_by_role, '')
    );
  end if;

  return true;
end;
$$;

grant execute on function public.officer_sync_report_workflow(
  bigint, text, text, text, text, text, text, text, text, text, text, text,
  text, text, text, bigint, text, text, text
) to authenticated;

-- Restore the simple insert policy (audit writes now happen inside the RPC).
drop policy if exists officer_insert_status_history on public.status_history;
create policy officer_insert_status_history on public.status_history
  for insert to authenticated
  with check (public.is_officer());
