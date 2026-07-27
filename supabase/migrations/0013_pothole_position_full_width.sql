-- Document Full-width / crater lane code (F) for pothole_position.
-- Column already exists as text from 0012; no schema change required.
comment on column public.reports.pothole_position is
  'Reporter lane in close-up frame: L, M, R, or F (full-width / crater).';
