-- Idempotency log for citizen report confirmation emails (Edge Function).
-- One row per report_id after a successful Brevo send; prevents duplicate mail
-- when the Database Webhook retries.

create table if not exists public.report_email_log (
  report_id   bigint primary key references public.reports(id) on delete cascade,
  recipient   text not null,
  ticket      text not null,
  sent_at     timestamptz not null default now()
);

create index if not exists idx_report_email_log_sent_at
  on public.report_email_log (sent_at desc);

comment on table public.report_email_log is
  'Tracks report submission confirmation emails sent by notify-citizen-report-created.';

alter table public.report_email_log enable row level security;

-- No policies: only service_role (Edge Function) can read/write.

grant select, insert, delete on public.report_email_log to service_role;
