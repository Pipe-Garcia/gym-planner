UPDATE users
SET active = false,
    password_hash = '$2a$12$lr2Au8nwU2xU/WcHIktfSe1fmgb8DB64feYDRKJlUWIma3gRInhP6',
    email = 'disabled-bootstrap-owner@gymplanner.invalid',
    full_name = 'Disabled Bootstrap Owner',
    updated_at = now()
WHERE email = 'admin@gymplanner.local'
  AND password_hash = '$2a$12$4R6qzOmnCNaUu.BZUknvQOoc3khx2pQJO32mouS8JA/nljpHelqUi';
