-- Tags adicionales detectados como faltantes durante uso real.
-- Idempotente: solo inserta si no existe el slug.

INSERT INTO exercise_tags (gym_id, type, name, slug)
SELECT 1, 'OBJECTIVE', 'Estabilidad', 'estabilidad'
WHERE NOT EXISTS (
    SELECT 1 FROM exercise_tags
    WHERE gym_id = 1 AND type = 'OBJECTIVE' AND slug = 'estabilidad'
);
