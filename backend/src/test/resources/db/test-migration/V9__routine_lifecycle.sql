ALTER TABLE routines ADD COLUMN finished_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE routines ADD COLUMN finished_by_user_id BIGINT REFERENCES users(id);
ALTER TABLE routines ADD COLUMN previous_routine_id BIGINT REFERENCES routines(id) ON DELETE SET NULL;
ALTER TABLE routines ADD COLUMN closure_notes TEXT;

CREATE INDEX idx_routines_previous ON routines(previous_routine_id);
