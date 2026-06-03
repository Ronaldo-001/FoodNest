# FoodWise — Microservices Platform

> Smart food inventory and surplus alert platform for restaurants

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│   React Frontend (port 3000)                                 │
│   Nginx + Vite + React Router + React Query                  │
└────────────────────┬─────────────────────────────────────────┘
                     │ Bearer JWT
         ┌───────────┼──────────────────────┐
         ▼           ▼                      ▼
┌──────────────┐ ┌──────────────────┐ ┌──────────────────────┐
│ auth-service │ │ catalog-order-   │ │ inventory-notif-     │
│  (port 8081) │ │  service         │ │  service             │
│              │ │  (port 8082)     │ │                      │
│ Spring Boot  │ │  Spring Boot     │ │  inventory-app       │
│ PostgreSQL   │ │  PostgreSQL      │ │  (port 8083)         │
│ Redis        │ │  Redis           │ │  PostgreSQL, Redis   │
│              │ │                  │ │                      │
│              │ │                  │ │  notification-worker │
│              │ │                  │ │  (background worker) │
└──────────────┘ └──────────────────┘ └──────────────────────┘
         │                │                      │
         └────────────────┴──INTERNAL_SERVICE_TOKEN──┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| `auth-service` | 8081 | JWT auth, register/login/logout/refresh/validate |
| `catalog-order-service` | 8082 | Menu CRUD, order lifecycle |
| `inventory-app` | 8083 | Inventory tracking, surplus detection |
| `notification-worker` | — | Background email dispatcher |
| `frontend` | 3000 | React SPA |

## Quick Start

```bash
# 1. Copy environment file
cp .env.example .env
# Edit .env — at minimum set JWT_SECRET and INTERNAL_SERVICE_TOKEN

# Generate secrets
openssl rand -hex 32  # for JWT_SECRET
openssl rand -hex 24  # for INTERNAL_SERVICE_TOKEN

# 2. Start everything
docker compose up --build

# 3. Access
open http://localhost:3000    # Frontend
open http://localhost:8025    # Mailhog (dev email UI)
```

## Environment Variables

See [.env.example](.env.example) for all required variables.

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | Random 32+ byte hex string |
| `INTERNAL_SERVICE_TOKEN` | Shared inter-service auth token |
| `AUTH_POSTGRES_PASSWORD` | auth-service DB password |
| `CATALOG_POSTGRES_PASSWORD` | catalog-order-service DB password |
| `INVENTORY_POSTGRES_PASSWORD` | inventory-notification-service DB password |

## Development (without Docker)

```bash
# Start only infra dependencies
docker compose up auth-postgres catalog-postgres inventory-postgres \
                  auth-redis catalog-redis inventory-redis mailhog

# Run each service locally
cd auth-service && mvn spring-boot:run
cd catalog-order-service && mvn spring-boot:run
cd inventory-notification-service/inventory-app && mvn spring-boot:run
cd inventory-notification-service/notification-worker && mvn spring-boot:run
cd frontend && cp .env.example .env.local && npm install && npm run dev
```

## Security Notes

- JWT: HS256, 15 min access token, rotation on every refresh, JTI blacklist on logout
- Token stored in React memory only (not localStorage)
- BCrypt password hashing (strength 10)
- Rate limiting: 5 logins/60s, 3 registrations/1h per IP (Redis-backed)
- Generic auth error messages (prevents user enumeration)
- Internal service calls: shared INTERNAL_SERVICE_TOKEN (→ replace with Istio mTLS in K8s)
- Security headers: X-Frame-Options DENY, X-Content-Type-Options, Referrer-Policy
- CORS: strict origin allowlist from env var
