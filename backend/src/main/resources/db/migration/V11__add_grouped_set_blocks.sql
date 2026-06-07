ALTER TABLE template_blocks DROP CONSTRAINT IF EXISTS template_blocks_structural_type_check;
ALTER TABLE template_blocks
    ADD CONSTRAINT template_blocks_structural_type_check
    CHECK (structural_type IN
           ('STANDARD','CIRCUIT','GROUPED_SET','PYRAMID','REVERSE_PYRAMID','DROP_SET','REST_PAUSE','CLUSTER'));

ALTER TABLE routine_blocks DROP CONSTRAINT IF EXISTS routine_blocks_structural_type_check;
ALTER TABLE routine_blocks
    ADD CONSTRAINT routine_blocks_structural_type_check
    CHECK (structural_type IN
           ('STANDARD','CIRCUIT','GROUPED_SET','PYRAMID','REVERSE_PYRAMID','DROP_SET','REST_PAUSE','CLUSTER'));

ALTER TABLE template_blocks ADD COLUMN round_rest_seconds INTEGER NULL;
ALTER TABLE routine_blocks ADD COLUMN round_rest_seconds INTEGER NULL;
