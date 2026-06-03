-- notification-worker uses the same inventory_db schema as inventory-app
-- Flyway validates the existing schema (managed by inventory-app)
-- This placeholder ensures Flyway baseline is consistent

SELECT 1; -- No-op: schema managed by inventory-app
