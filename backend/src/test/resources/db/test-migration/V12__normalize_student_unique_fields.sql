UPDATE students
SET document_id = NULLIF(regexp_replace(document_id, '[^0-9]', '', 'g'), '')
WHERE document_id IS NOT NULL;

UPDATE students
SET email = NULLIF(lower(btrim(email)), '')
WHERE email IS NOT NULL;

ALTER TABLE students
    ADD CONSTRAINT uk_students_gym_email UNIQUE (gym_id, email);
