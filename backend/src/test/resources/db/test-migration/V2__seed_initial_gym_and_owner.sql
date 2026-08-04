INSERT INTO gyms (id, name, owner_name, phone, email, address, logo_url, primary_color)
VALUES (1, 'Gym Planner Test', 'Test Owner', '+54 11 0000-0000', 'owner@test.local', 'Test 123', NULL, '#2563EB');

INSERT INTO users (id, gym_id, email, password_hash, full_name, role, active)
VALUES (
    1,
    1,
    'owner@test.local',
    '$2a$12$4R6qzOmnCNaUu.BZUknvQOoc3khx2pQJO32mouS8JA/nljpHelqUi',
    'Test Owner',
    'OWNER',
    true
);
