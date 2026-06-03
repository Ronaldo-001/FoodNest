# FoodWise — Architecture Reference

## System Overview

FoodWise is a 3-microservice application running on Docker Compose (local) and designed for Kubernetes deployment. Each service is independently deployed, owns its own database, and communicates over HTTP.

---

## Service Architecture Diagram

```
                          ┌──────────────────────────────────┐
                          │         React Frontend           │
                          │  Vite + React Router + React     │
                          │  Query + Axios  (port 3000)      │
                          └──────────────┬───────────────────┘
                                         │ HTTPS / Bearer JWT
               ┌─────────────────────────┼─────────────────────────┐
               │                         │                         │
               ▼                         ▼                         ▼
  ┌────────────────────┐   ┌──────────────────────┐   ┌────────────────────────┐
  │   auth-service     │   │ catalog-order-service│   │ inventory-app          │
  │   port 8081        │   │ port 8082            │   │ port 8083              │
  │                    │   │                      │   │                        │
  │ POST /auth/register│   │ GET  /menu/items     │   │ GET  /inventory/items  │
  │ POST /auth/login   │   │ POST /menu/items     │   │ POST /inventory/items  │
  │ POST /auth/logout  │   │ PUT  /menu/items/:id │   │ POST /inventory/deduct │
  │ POST /auth/refresh │   │ DELETE /menu/items/:i│   │ POST /inventory/restore│
  │ GET  /auth/validate│◄──│ POST /orders         │   │ GET  /inventory/surplus│
  │                    │   │ GET  /orders/:id     │   │ POST /notif/subscribe  │
  │ PostgreSQL (auth)  │   │ PATCH /orders/:id/st │──►│                        │
  │ Redis (auth)       │   │                      │   │ PostgreSQL (inventory) │
  └────────────────────┘   │ PostgreSQL (catalog) │   │ Redis (inventory)      │
                           │ Redis (catalog)      │   └────────────────────────┘
                           └──────────────────────┘             │
                                                                 │ (same DB)
                                                                 ▼
                                                    ┌────────────────────────┐
                                                    │ notification-worker    │
                                                    │ (no HTTP — background) │
                                                    │                        │
                                                    │ Polls surplus_alerts   │
                                                    │ Dispatches emails via  │
                                                    │ Spring Mail → Mailhog  │
                                                    └────────────────────────┘
```

---

## Data Flow: Customer Places an Order

```
1. Customer logs in
   POST /auth/login → auth-service
   ← 200 { accessToken, refreshToken }

2. Customer browses menu
   GET /menu/items → catalog-order-service
   → catalog-order validates JWT: GET /auth/validate → auth-service
   ← 200 { content: [ MenuItem ] }

3. Customer places order
   POST /orders → catalog-order-service
   → Validates each menu item belongs to restaurant
   → Calculates total with price snapshot
   → Saves order (PENDING)
   → POST /inventory/deduct → inventory-app (with INTERNAL_SERVICE_TOKEN)
   → inventory-app checks surplus condition, creates SurplusAlert if needed
   ← 201 { OrderDto }

4. notification-worker polls every 60s
   → Finds unnotified SurplusAlerts
   → Finds subscribers for that restaurant
   → Sends plain-text email via Mailhog
   → Marks alert as notified
```

---

## Data Flow: Restaurant Updates Inventory

```
1. Owner logs in → POST /auth/login → 200 { accessToken }

2. Owner views inventory
   GET /inventory/items → inventory-app
   → auth-service validates JWT, returns { roles: ["RESTAURANT_OWNER"], restaurantId: 42 }
   ← inventory items for restaurantId=42

3. Owner updates stock
   PUT /inventory/items/7 { quantity: 5 }
   → inventory-app saves
   → SurplusDetectionService.checkAndMarkSurplus(item)
     → quantity(5) < threshold(10) → LOW_STOCK
     → item.surplus = true
     → SurplusAlert created (notified=false)

4. notification-worker fires on next poll
   → Sends emails to subscribers of restaurant 42
```

---

## Token Flow

```
Login Response:
{
  accessToken:  "eyJ..." (JWT, 15 min, HS256)
  refreshToken: "uuid" (stored in DB, rotates on use)
  expiresIn:    900
}

Access Token Claims:
{
  sub:      "42"              // userId
  jti:      "random-uuid"    // for blacklisting on logout
  username: "jane_owner"
  roles:    ["RESTAURANT_OWNER"]
  iat / exp
}

On logout:
  → All refresh tokens for user are deleted from DB
  → Access token JTI added to Redis blacklist (TTL = remaining expiry)
  → /auth/validate checks blacklist → returns { valid: false }
```

---

## Database Schema Overview

### auth_db

```
users
  id, username, email, password_hash, restaurant_id, active, created_at

roles
  id, name (CUSTOMER | RESTAURANT_OWNER | ADMIN)

user_roles (join table)
  user_id, role_id

refresh_tokens
  id, user_id, token, expires_at, revoked, created_at
```

### catalog_db

```
menu_items
  id, restaurant_id, name, description, price, category, available, image_url

orders
  id, customer_id, restaurant_id, status (enum), total_amount, notes

order_items
  id, order_id, menu_item_id, menu_item_name, quantity, unit_price, subtotal
  ← price snapshot prevents historical drift
```

### inventory_db

```
inventory_items
  id, restaurant_id, menu_item_id, name, quantity, unit, threshold,
  expiry_date, is_surplus

surplus_alerts
  id, inventory_item_id, restaurant_id, reason (LOW_STOCK|EXPIRY_SOON),
  triggered_at, notified, notified_at

notification_subscriptions
  id, customer_id, customer_email, restaurant_id

notification_logs
  id, alert_id, customer_email, subject, status (SENT|FAILED), sent_at
```

---

## Inter-Service Communication

| Caller | Callee | Endpoint | Auth |
|--------|--------|----------|------|
| catalog-order | auth-service | `GET /auth/validate` | Bearer (user JWT) |
| inventory-app | auth-service | `GET /auth/validate` | Bearer (user JWT) |
| catalog-order | inventory-app | `POST /inventory/deduct` | Bearer (INTERNAL_SERVICE_TOKEN) |
| catalog-order | inventory-app | `POST /inventory/restore` | Bearer (INTERNAL_SERVICE_TOKEN) |
| notification-worker | inventory_db | Direct DB connection | DB credentials |

---

## Port Map

| Container | Port | Description |
|-----------|------|-------------|
| auth-service | 8081 | Auth REST API |
| catalog-order-service | 8082 | Catalog & Order REST API |
| inventory-app | 8083 | Inventory REST API |
| notification-worker | — | No HTTP exposure |
| frontend | 3000 | React SPA (dev server) |
| auth-postgres | 5432 (mapped 5433) | Auth PostgreSQL |
| catalog-postgres | 5432 (mapped 5434) | Catalog PostgreSQL |
| inventory-postgres | 5432 (mapped 5435) | Inventory PostgreSQL |
| auth-redis | 6379 (mapped 6380) | Auth Redis |
| catalog-redis | 6379 (mapped 6381) | Catalog Redis |
| inventory-redis | 6379 (mapped 6382) | Inventory Redis |
| mailhog | 1025 / 8025 | SMTP / Web UI |
