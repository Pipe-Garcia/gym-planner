-- =================================================================
-- V8 - Multi-day templates and routines
-- Agrega un nivel "Día" entre template/routine y sus bloques.
-- Mantiene compatibilidad con datos existentes creando un "Día 1"
-- automático por cada template/routine y reapuntando los bloques.
-- =================================================================

-- ============== TEMPLATE_DAYS ==============

CREATE TABLE template_days (
    id              BIGSERIAL PRIMARY KEY,
    template_id     BIGINT NOT NULL REFERENCES training_templates(id) ON DELETE CASCADE,
    order_index     INTEGER NOT NULL,
    name            VARCHAR(150) NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_template_days_order UNIQUE (template_id, order_index)
);

CREATE INDEX idx_template_days_template ON template_days(template_id, order_index);

-- ============== ROUTINE_DAYS ==============

CREATE TABLE routine_days (
    id              BIGSERIAL PRIMARY KEY,
    routine_id      BIGINT NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
    order_index     INTEGER NOT NULL,
    name            VARCHAR(150) NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_routine_days_order UNIQUE (routine_id, order_index)
);

CREATE INDEX idx_routine_days_routine ON routine_days(routine_id, order_index);

-- ============== MIGRAR DATOS EXISTENTES - TEMPLATES ==============

INSERT INTO template_days (template_id, order_index, name)
SELECT id, 1, 'Día 1' FROM training_templates;

ALTER TABLE template_blocks ADD COLUMN template_day_id BIGINT;

UPDATE template_blocks tb
SET template_day_id = (
    SELECT td.id FROM template_days td
    WHERE td.template_id = tb.template_id AND td.order_index = 1
);

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM template_blocks WHERE template_day_id IS NULL) THEN
    RAISE EXCEPTION 'Migración V8 falló: hay template_blocks sin template_day_id';
  END IF;
END $$;

ALTER TABLE template_blocks ALTER COLUMN template_day_id SET NOT NULL;

ALTER TABLE template_blocks
    ADD CONSTRAINT fk_template_blocks_day
    FOREIGN KEY (template_day_id) REFERENCES template_days(id) ON DELETE CASCADE;

ALTER TABLE template_blocks DROP CONSTRAINT IF EXISTS uk_tb_template_order;

ALTER TABLE template_blocks
    ADD CONSTRAINT uk_template_blocks_day_order UNIQUE (template_day_id, order_index);

ALTER TABLE template_blocks DROP CONSTRAINT IF EXISTS template_blocks_template_id_fkey;
ALTER TABLE template_blocks DROP COLUMN template_id;

DROP INDEX IF EXISTS idx_template_blocks_template;

CREATE INDEX idx_template_blocks_day ON template_blocks(template_day_id, order_index);

-- ============== MIGRAR DATOS EXISTENTES - ROUTINES ==============

INSERT INTO routine_days (routine_id, order_index, name)
SELECT id, 1, 'Día 1' FROM routines;

ALTER TABLE routine_blocks ADD COLUMN routine_day_id BIGINT;

UPDATE routine_blocks rb
SET routine_day_id = (
    SELECT rd.id FROM routine_days rd
    WHERE rd.routine_id = rb.routine_id AND rd.order_index = 1
);

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM routine_blocks WHERE routine_day_id IS NULL) THEN
    RAISE EXCEPTION 'Migración V8 falló: hay routine_blocks sin routine_day_id';
  END IF;
END $$;

ALTER TABLE routine_blocks ALTER COLUMN routine_day_id SET NOT NULL;

ALTER TABLE routine_blocks
    ADD CONSTRAINT fk_routine_blocks_day
    FOREIGN KEY (routine_day_id) REFERENCES routine_days(id) ON DELETE CASCADE;

ALTER TABLE routine_blocks DROP CONSTRAINT IF EXISTS uk_rb_routine_order;

ALTER TABLE routine_blocks
    ADD CONSTRAINT uk_routine_blocks_day_order UNIQUE (routine_day_id, order_index);

ALTER TABLE routine_blocks DROP CONSTRAINT IF EXISTS routine_blocks_routine_id_fkey;
ALTER TABLE routine_blocks DROP COLUMN routine_id;

DROP INDEX IF EXISTS idx_routine_blocks_routine;
CREATE INDEX idx_routine_blocks_day ON routine_blocks(routine_day_id, order_index);
