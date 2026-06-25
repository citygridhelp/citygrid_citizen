-- ============================================================================
-- email_exists(): lets the citizen app distinguish "no account yet" from
-- "wrong password" on login. Supabase hides this by default to prevent email
-- enumeration; this RPC exposes only a boolean and is acceptable for this app.
--
-- Run in the Supabase SQL Editor after 0001_init.sql.
-- ============================================================================

create or replace function public.email_exists(p_email text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from auth.users
    where lower(email) = lower(trim(p_email))
  );
$$;

-- Callable by signed-out (anon) users from the login screen.
grant execute on function public.email_exists(text) to anon, authenticated;
