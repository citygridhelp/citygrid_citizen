-- Prevent workflow sync from reverting commissioner-published completion.
-- After publish, citizen_visible_status must stay COMPLETED even if a stale
-- officer_sync_report_workflow call still sends IN_PROGRESS.

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
  next_citizen_visible report_status;
begin
  select * into o from public.gov_officers where auth_id = auth.uid();
  if o.auth_id is null then
    raise exception 'caller is not a registered officer';
  end if;

  select case
    when r.citizen_visible_status = 'COMPLETED'
      and coalesce(r.completed_at_ms, 0) > 0
      then r.citizen_visible_status
    else p_citizen_visible_status::report_status
  end
  into next_citizen_visible
  from public.reports r
  where r.id = p_report_id
    and r.city_key = o.city_key;

  if not found then
    return false;
  end if;

  update public.reports r
  set
    status = p_status::report_status,
    citizen_visible_status = next_citizen_visible,
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

-- Also set internal status when commissioner publishes (keeps columns aligned).
create or replace function public.officer_publish_citizen_completion(
  p_report_id bigint,
  p_top_path text,
  p_wide_path text,
  p_commissioner_note text default '',
  p_published_by_key text default '',
  p_published_by_name text default '',
  p_completed_at_ms bigint default null
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
  if o.auth_id is null or o.role <> 'COMMISSIONER' then
    raise exception 'only a city commissioner may publish citizen completion';
  end if;

  update public.reports r
  set
    status = 'COMPLETED',
    citizen_visible_status = 'COMPLETED',
    completion_top_path = p_top_path,
    completion_wide_path = p_wide_path,
    commissioner_completion_note = coalesce(p_commissioner_note, ''),
    completed_at_ms = coalesce(
      p_completed_at_ms,
      (extract(epoch from now()) * 1000)::bigint
    ),
    published_by_key = coalesce(p_published_by_key, ''),
    published_by_name = coalesce(p_published_by_name, ''),
    updated_at = now()
  where r.id = p_report_id
    and r.city_key = o.city_key;

  get diagnostics rows_updated = row_count;
  return rows_updated > 0;
end;
$$;
