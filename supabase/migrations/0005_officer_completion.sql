-- Phase 6: completion proof metadata + commissioner publish to citizen.

-- ---------------------------------------------------------------------------
-- 1. Upsert a completion proof row (after Storage upload from the govt app)
-- ---------------------------------------------------------------------------
create or replace function public.officer_upsert_completion_proof(
  p_report_id bigint,
  p_kind text,
  p_storage_path text,
  p_uploaded_by_key text default ''
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  o public.gov_officers;
begin
  select * into o from public.gov_officers where auth_id = auth.uid();
  if o.auth_id is null then
    raise exception 'caller is not a registered officer';
  end if;

  if not exists (
    select 1 from public.reports r
    where r.id = p_report_id and r.city_key = o.city_key
  ) then
    return false;
  end if;

  delete from public.report_proofs
  where report_id = p_report_id and kind = p_kind;

  insert into public.report_proofs (
    report_id, kind, storage_path, uploaded_by_key, created_at_ms
  ) values (
    p_report_id,
    p_kind,
    p_storage_path,
    coalesce(p_uploaded_by_key, ''),
    (extract(epoch from now()) * 1000)::bigint
  );

  return true;
end;
$$;

grant execute on function public.officer_upsert_completion_proof(
  bigint, text, text, text
) to authenticated;

-- ---------------------------------------------------------------------------
-- 2. Commissioner publishes completion to the citizen-visible report row
-- ---------------------------------------------------------------------------
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

grant execute on function public.officer_publish_citizen_completion(
  bigint, text, text, text, text, text, bigint
) to authenticated;

-- Citizens may read repair proof objects (bucket was officer-insert only).
drop policy if exists proofs_read on storage.objects;
create policy proofs_read on storage.objects
  for select to authenticated
  using (bucket_id in ('proofs', 'evidence'));
