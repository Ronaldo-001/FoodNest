-- V2: Seed default roles
INSERT INTO roles (name) VALUES
    ('CUSTOMER'),
    ('RESTAURANT_OWNER'),
    ('ADMIN')
ON CONFLICT (name) DO NOTHING;
