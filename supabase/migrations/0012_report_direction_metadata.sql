-- Lane position + traffic-facing metadata for trip navigation (citizen reports).
alter table public.reports
  add column if not exists pothole_position text not null default '',
  add column if not exists report_bearing_deg real,
  add column if not exists traffic_bearing_deg real,
  add column if not exists traffic_faces_camera boolean;

comment on column public.reports.pothole_position is
  'Reporter lane in close-up frame: L, M, or R (PotholePosition.code).';
comment on column public.reports.report_bearing_deg is
  'Device/GPS heading at submit (0–360°), when available.';
comment on column public.reports.traffic_bearing_deg is
  'Direction vehicles travel toward (0–360°), from wide-shot confirm.';
comment on column public.reports.traffic_faces_camera is
  'True when traffic flows toward camera top at wide-shot confirm; null = unknown.';
