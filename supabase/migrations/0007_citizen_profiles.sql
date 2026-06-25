-- Stable citizen reporter_user_id (PW-xxx) keyed by Supabase auth, so the same
-- email/account keeps one privacy id across devices and reinstalls.

create table if not exists public.citizen_profiles (
  auth_id          uuid primary key references auth.users(id) on delete cascade,
  reporter_user_id text not null,
  email            text not null default '',
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);

create unique index if not exists idx_citizen_profiles_reporter_user_id
  on public.citizen_profiles (reporter_user_id);

drop trigger if exists trg_citizen_profiles_touch on public.citizen_profiles;
create trigger trg_citizen_profiles_touch
  before update on public.citizen_profiles
  for each row execute function public.touch_updated_at();

alter table public.citizen_profiles enable row level security;

drop policy if exists citizen_profile_self on public.citizen_profiles;
create policy citizen_profile_self on public.citizen_profiles
  for all to authenticated
  using (auth_id = auth.uid())
  with check (auth_id = auth.uid());

-- Returns the canonical reporter_user_id for auth.uid().
-- Priority: profile row -> latest report row -> unused p_desired -> deterministic PW-{auth}.
create or replace function public.citizen_reporter_user_id(p_desired text default '')
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  result text;
  desired text := nullif(trim(p_desired), '');
begin
  if uid is null then
    raise exception 'not authenticated';
  end if;

  select cp.reporter_user_id into result
  from public.citizen_profiles cp
  where cp.auth_id = uid;

  if result is not null then
    return result;
  end if;

  select r.reporter_user_id into result
  from public.reports r
  where r.reporter_auth_id = uid
    and r.reporter_user_id <> ''
  order by r.created_at_ms desc
  limit 1;

  if result is null
     and desired is not null
     and not exists (
       select 1 from public.citizen_profiles cp where cp.reporter_user_id = desired
     ) then
    result := desired;
  end if;

  if result is null then
    result := 'PW-' || upper(substr(replace(uid::text, '-', ''), 1, 8));
  end if;

  insert into public.citizen_profiles (auth_id, reporter_user_id, email)
  values (
    uid,
    result,
    coalesce((select email from auth.users where id = uid), '')
  )
  on conflict (auth_id) do update
    set reporter_user_id = excluded.reporter_user_id,
        updated_at = now()
  returning reporter_user_id into result;

  return result;
end;
$$;

grant execute on function public.citizen_reporter_user_id(text) to authenticated;
