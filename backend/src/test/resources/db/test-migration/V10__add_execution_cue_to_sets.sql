ALTER TABLE template_exercise_sets
    ADD COLUMN execution_cue VARCHAR(120) NULL;

ALTER TABLE routine_exercise_sets
    ADD COLUMN execution_cue VARCHAR(120) NULL;
