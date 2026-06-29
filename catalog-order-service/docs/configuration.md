# Configuration — Catalog & Order Service

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | — | **Required.** Must match the secret used by `auth-service`. |
| `INTERNAL_SERVICE_TOKEN` | — | **Required.** Shared token for calling `inventory-app`. Generate with `openssl rand -hex 24`. |
| `CATALOG_DB_URL` | — | JDBC URL, e.g. `jdbc:postgresql://localhost:5434/catalog_db` |
| `CATALOG_DB_USER` | — | PostgreSQL username |
| `CATALOG_DB_PASSWORD` | — | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6381` | Redis port |
| `AUTH_SERVICE_URL` | `http://auth-service:8081` | Auth service base URL |
| `INVENTORY_SERVICE_URL` | `http://inventory-app:8083` | Inventory service base URL |
| `SERVER_PORT` | `8082` | HTTP port |

## application.yml (key excerpts)

```yaml
server:
  port: ${SERVER_PORT:8082}

app:
  auth-service-url: ${AUTH_SERVICE_URL:http://auth-service:8081}
  inventory-service-url: ${INVENTORY_SERVICE_URL:http://inventory-app:8083}
  internal-service-token: ${INTERNAL_SERVICE_TOKEN}
  cache:
    menu-item-ttl-seconds: 300
```

## WebClient Timeouts

The async `WebClient` used for inter-service calls is configured with:

| Property | Value |
|----------|-------|
| Connect timeout | 3 seconds |
| Read timeout | 5 seconds |

Requests that exceed these limits will fail with a timeout error. On inventory calls, the order proceeds anyway (best-effort).

## Generating Secrets

```bash
# INTERNAL_SERVICE_TOKEN
openssl rand -hex 24
```
