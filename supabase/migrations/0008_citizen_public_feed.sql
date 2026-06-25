-- Citizen app: public recent reports + map feed for guests and fresh installs.
-- Anon and signed-in citizens can read city reports; evidence/proof photos are served
-- from public buckets (object paths are report-id based and unguessable).

grant select on public.reports to anon;

drop policy if exists public_city_reports_read on public.reports;
create policy public_city_reports_read on public.reports
  for select to anon, authenticated
  using (true);

update storage.buckets set public = true where id in ('evidence', 'proofs');
