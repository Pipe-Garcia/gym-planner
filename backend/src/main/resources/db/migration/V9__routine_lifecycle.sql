ALTER TABLE routines
  ADD COLUMN finished_at         TIMESTAMPTZ,
  ADD COLUMN finished_by_user_id BIGINT REFERENCES users(id),
  ADD COLUMN previous_routine_id BIGINT REFERENCES routines(id) ON DELETE SET NULL,
  ADD COLUMN closure_notes       TEXT;

CREATE INDEX idx_routines_previous ON routines(previous_routine_id);
