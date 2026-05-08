INSERT INTO gyms (id, name, owner_name, phone, email, address, logo_url, primary_color)
VALUES (1, 'Gym Planner Demo', 'Owner Demo', '+54 11 0000-0000', 'admin@gymplanner.local', 'Demo 123', NULL, '#2563EB');

INSERT INTO users (id, gym_id, email, password_hash, full_name, role, active)
VALUES (
    1,
    1,
    'admin@gymplanner.local',
    '$2a$12$4R6qzOmnCNaUu.BZUknvQOoc3khx2pQJO32mouS8JA/nljpHelqUi',
    'Owner Demo',
    'OWNER',
    true
);
