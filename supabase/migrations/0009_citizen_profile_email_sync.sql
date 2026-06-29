-- Keep citizen_profiles.email in sync with auth.users.email.
-- Identity for reports is auth_id + reporter_user_id (PW-xxx), not the email string.

-- 1) Refresh email whenever the profile RPC runs (sign-in / report submit).
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
  auth_email text;
begin
  if uid is null then
    raise exception 'not authenticated';
  end if;

  select lower(trim(u.email)) into auth_email
  from auth.users u
  where u.id = uid;

  select cp.reporter_user_id into result
  from public.citizen_profiles cp
  where cp.auth_id = uid;

  if result is not null then
    update public.citizen_profiles cp
    set email = coalesce(auth_email, cp.email),
        updated_at = now()
    where cp.auth_id = uid
      and coalesce(auth_email, '') <> ''
      and lower(trim(cp.email)) is distinct from auth_email;
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
    coalesce(auth_email, '')
  )
  on conflict (auth_id) do update
    set reporter_user_id = excluded.reporter_user_id,
        email = coalesce(auth_email, citizen_profiles.email),
        updated_at = now()
  returning reporter_user_id into result;

  return result;
end;
$$;

-- 2) Trigger: when Auth email changes, update the profile row immediately.
create or replace function public.sync_citizen_profile_email_from_auth()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if new.email is null or trim(new.email) = '' then
    return new;
  end if;

  update public.citizen_profiles
  set email = lower(trim(new.email)),
      updated_at = now()
  where auth_id = new.id;

  return new;
end;
$$;

drop trigger if exists trg_auth_users_sync_citizen_email on auth.users;
create trigger trg_auth_users_sync_citizen_email
  after insert or update of email on auth.users
  for each row
  execute function public.sync_citizen_profile_email_from_auth();

-- 3) One-time backfill for rows already out of date.
update public.citizen_profiles cp
set email = lower(trim(u.email)),
    updated_at = now()
from auth.users u
where u.id = cp.auth_id
  and coalesce(u.email, '') <> ''
  and lower(trim(cp.email)) is distinct from lower(trim(u.email));
