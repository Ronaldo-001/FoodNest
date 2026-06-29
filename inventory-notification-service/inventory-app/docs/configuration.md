# Configuration — Inventory Service

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | — | **Required.** Must match auth-service. |
| `INTERNAL_SERVICE_TOKEN` | — | **Required.** Must match catalog-order-service. |
| `INVENTORY_DB_URL` | — | JDBC URL, e.g. `jdbc:postgresql://localhost:5435/inventory_db` |
| `INVENTORY_DB_USER` | — | PostgreSQL username |
| `INVENTORY_DB_PASSWORD` | — | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6382` | Redis port |
| `AUTH_SERVICE_URL` | `http://auth-service:8081` | Auth service base URL |
| `SURPLUS_QUANTITY_THRESHOLD` | `10` | Default low-stock threshold |
| `SURPLUS_EXPIRY_HOURS` | `24` | Hours before expiry to flag `EXPIRY_SOON` |
| `SERVER_PORT` | `8083` | HTTP port |

## Shared Database

The `inventory_db` PostgreSQL database is shared between `inventory-app` and `notification-worker`. Schema migrations are owned exclusively by `inventory-app` (Flyway). The `notification-worker` has read/write access but never runs migrations.

## application.yml (key excerpts)

```yaml
server:
  port: ${SERVER_PORT:8083}

app:
  surplus:
    quantity-threshold: ${SURPLUS_QUANTITY_THRESHOLD:10}
    expiry-hours: ${SURPLUS_EXPIRY_HOURS:24}
  auth-service-url: ${AUTH_SERVICE_URL:http://auth-service:8081}
  internal-service-token: ${INTERNAL_SERVICE_TOKEN}
```
