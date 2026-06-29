# Configuration — Auth Service

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | — | **Required.** HS256 signing key. Generate with `openssl rand -hex 32`. Must be ≥32 bytes. |
| `JWT_ACCESS_EXPIRY_MS` | `900000` | Access token TTL in milliseconds (default: 15 min) |
| `JWT_REFRESH_EXPIRY_MS` | `604800000` | Refresh token TTL in milliseconds (default: 7 days) |
| `AUTH_DB_URL` | — | JDBC URL for PostgreSQL, e.g. `jdbc:postgresql://localhost:5433/auth_db` |
| `AUTH_DB_USER` | — | PostgreSQL username |
| `AUTH_DB_PASSWORD` | — | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6380` | Redis port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated list of allowed CORS origins |
| `SERVER_PORT` | `8081` | HTTP port the service listens on |

## application.yml (key excerpts)

```yaml
server:
  port: ${SERVER_PORT:8081}

spring:
  datasource:
    url: ${AUTH_DB_URL}
    username: ${AUTH_DB_USER}
    password: ${AUTH_DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6380}

app:
  jwt:
    secret: ${JWT_SECRET}
    access-expiry-ms: ${JWT_ACCESS_EXPIRY_MS:900000}
    refresh-expiry-ms: ${JWT_REFRESH_EXPIRY_MS:604800000}
  rate-limit:
    login:
      max: 5
      window-seconds: 60
    register:
      max: 3
      window-seconds: 3600
```

## Generating Secrets

```bash
# JWT secret (≥32 bytes)
openssl rand -hex 32

# Example output
3f8a2b1d9e4c7f6a0b5d3e2f1a8c4b7d9e6f3a2b1d9e4c7f6a0b5d3e2f1a8c
```

## Docker Compose

```yaml
auth-service:
  build: ./auth-service
  ports:
    - "8081:8081"
  environment:
    JWT_SECRET: ${JWT_SECRET}
    AUTH_DB_URL: jdbc:postgresql://auth-postgres:5432/auth_db
    AUTH_DB_USER: auth_user
    AUTH_DB_PASSWORD: ${AUTH_DB_PASSWORD}
    REDIS_HOST: auth-redis
    REDIS_PORT: 6379
  networks:
    - auth-net
    - shared-net
```
