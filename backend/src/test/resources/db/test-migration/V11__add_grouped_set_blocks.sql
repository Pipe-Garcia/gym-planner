ALTER TABLE template_blocks DROP COLUMN structural_type;
ALTER TABLE template_blocks
    ADD COLUMN structural_type VARCHAR(30) NOT NULL CHECK (structural_type IN
        ('STANDARD','CIRCUIT','GROUPED_SET','PYRAMID','REVERSE_PYRAMID','DROP_SET','REST_PAUSE','CLUSTER'));

ALTER TABLE routine_blocks DROP COLUMN structural_type;
ALTER TABLE routine_blocks
    ADD COLUMN structural_type VARCHAR(30) NOT NULL CHECK (structural_type IN
        ('STANDARD','CIRCUIT','GROUPED_SET','PYRAMID','REVERSE_PYRAMID','DROP_SET','REST_PAUSE','CLUSTER'));

ALTER TABLE template_blocks ADD COLUMN round_rest_seconds INTEGER NULL;
ALTER TABLE routine_blocks ADD COLUMN round_rest_seconds INTEGER NULL;
