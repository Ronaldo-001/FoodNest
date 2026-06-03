# FoodWise — Application Flows & UI → Service Map

## Overview

FoodWise has two user roles, each with a distinct UI experience:

| Role | What they do | Landing page |
|---|---|---|
| `CUSTOMER` | Browse menus, place orders, subscribe to surplus alerts | `/browse` |
| `RESTAURANT_OWNER` | Manage menu items, track inventory, view surplus | `/dashboard` |

---

## Pages & Service Mapping

### `/login` — LoginPage

**UI:** Username/email + password form. On success, redirected based on role.

```
User submits form
  └─► POST /auth/login  →  auth-service :8081
        └─ validates credentials (BCrypt)
        └─ returns { accessToken, refreshToken, userId, username, roles, restaurantId }
  └─► Token stored in React memory (AuthContext), injected into all Axios instances
  └─► Redirect: RESTAURANT_OWNER → /dashboard, CUSTOMER → /browse
```

---

### `/register` — RegisterPage

**UI:** Username, email, password, role selector. Restaurant owners also enter a `restaurantId`.

```
User submits form
  └─► POST /auth/register  →  auth-service :8081
        └─ checks username/email uniqueness
        └─ BCrypt hashes password
        └─ assigns role (CUSTOMER or RESTAURANT_OWNER)
        └─ returns same token response as login
  └─► Auto-logged in, redirected to role home page
```

---

### `/browse` — CustomerBrowse (CUSTOMER only)

**UI:** Menu item grid with search by restaurant ID, shopping cart, surplus alert subscription.

```
Page loads
  └─► GET /menu/items?restaurantId=X&size=50  →  catalog-order-service :8082
        └─ catalog validates JWT:  GET /auth/validate  →  auth-service :8081
        └─ returns paginated menu items

Add to cart → client-side only (React state)

Place Order button
  └─► POST /orders  →  catalog-order-service :8082
        └─ validates all items belong to same restaurant
        └─ snapshots prices, calculates total
        └─ saves order (PENDING)
        └─► POST /inventory/deduct  →  inventory-app :8083  (INTERNAL_SERVICE_TOKEN)
              └─ reduces stock quantity
              └─ checks if quantity < threshold → creates SurplusAlert if so

Subscribe to Alerts button
  └─► POST /notifications/subscribe  →  inventory-app :8083
        └─ stores { customerId, customerEmail, restaurantId }
```

---

### `/dashboard` — RestaurantDashboard (RESTAURANT_OWNER only)

**UI:** Two-tab interface — Menu Management and Inventory Management. Surplus badge in header.

#### Tab 1: Menu Management

```
Page loads
  └─► GET /menu/items?restaurantId=X  →  catalog-order-service :8082

Create item button
  └─► POST /menu/items  →  catalog-order-service :8082
        └─ validates RESTAURANT_OWNER role
        └─ sets restaurantId from JWT (not from request body)

Edit item
  └─► PUT /menu/items/:id  →  catalog-order-service :8082
        └─ verifies item belongs to caller's restaurant

Delete item
  └─► DELETE /menu/items/:id  →  catalog-order-service :8082
        └─ verifies item belongs to caller's restaurant

All writes invalidate the Redis menu cache (key: menu:restaurant:{id})
```

#### Tab 2: Inventory Management

```
Page loads
  └─► GET /inventory/items  →  inventory-app :8083
        └─ inventory validates JWT:  GET /auth/validate  →  auth-service :8081
        └─ returns items for caller's restaurantId

  └─► GET /inventory/surplus  →  inventory-app :8083  (polled every 60s)
        └─ items where quantity < threshold OR expiryDate within 24h

Add inventory item
  └─► POST /inventory/items  →  inventory-app :8083
        └─ checks for duplicate (restaurantId + menuItemId)
        └─ SurplusDetectionService runs immediately after save

Update stock/threshold
  └─► PUT /inventory/items/:id  →  inventory-app :8083
        └─ SurplusDetectionService re-evaluates after update
        └─ if surplus: creates SurplusAlert (notified=false)
```

---

### `/orders` — OrderHistory

**UI:** Order cards with status badges. Customers see their own orders; owners see all orders for their restaurant.

```
Page loads (CUSTOMER)
  └─► GET /orders/customer/:userId  →  catalog-order-service :8082

Page loads (RESTAURANT_OWNER)
  └─► GET /orders/restaurant/:restaurantId  →  catalog-order-service :8082

Order status update (owner only)
  └─► PATCH /orders/:id/status  →  catalog-order-service :8082
        └─ valid transitions: PENDING → CONFIRMED → PREPARING → READY → DELIVERED
        └─ CANCELLED from any state
        └─ if CANCELLED: POST /inventory/restore  →  inventory-app :8083
```

---

### `/surplus` — SurplusAlerts

**UI:** Role-adaptive — owners see their surplus stock; customers manage alert subscriptions.

```
RESTAURANT_OWNER view (polled every 30s)
  └─► GET /inventory/surplus  →  inventory-app :8083
        └─ returns items flagged as surplus (low stock or expiry within threshold)

CUSTOMER view
  └─► GET /notifications/history  →  inventory-app :8083
        └─ returns notification log for this customer

Subscribe form (CUSTOMER)
  └─► POST /notifications/subscribe  →  inventory-app :8083
        └─ { restaurantId, customerEmail }
```

---

## Background Flow — Email Notifications

The `notification-worker` has no HTTP port. It runs a scheduled job every 60 seconds.

```
notification-worker polls inventory_db every 60s
  └─► SELECT surplus_alerts WHERE notified = false
  └─► For each alert:
        └─► SELECT notification_subscriptions WHERE restaurant_id = alert.restaurant_id
        └─► For each subscriber:
              └─► Send plain-text email via SMTP → Mailhog :1025
              └─► INSERT notification_logs (status=SENT or FAILED)
        └─► UPDATE surplus_alerts SET notified=true, notified_at=now()

View sent emails: http://localhost:8025  (Mailhog web UI)
```

---

## Token Lifecycle

```
Login/Register
  └─► accessToken  (JWT, HS256, 15 min) — sent as Bearer on every request
  └─► refreshToken (UUID, 7 days, stored in auth_db.refresh_tokens)

Every protected API call
  └─► receiving service calls GET /auth/validate (Bearer accessToken)
        └─► auth-service checks Redis blacklist (for logged-out tokens)
        └─► returns { valid, userId, username, roles, restaurantId }

Logout
  └─► POST /auth/logout  →  auth-service
        └─► deletes all refresh_tokens for user from DB
        └─► adds accessToken JTI to Redis blacklist (TTL = remaining expiry)
        └─► client clears token from memory, redirects to /login

Token in memory only — clears on page refresh (by design, no localStorage)
```

---

## Internal Service Calls (no user JWT)

```
catalog-order-service  →  inventory-app
  POST /inventory/deduct    (order placed)
  POST /inventory/restore   (order cancelled)

Auth: Bearer INTERNAL_SERVICE_TOKEN (shared secret, env var)
Security: inventory-app SecurityConfig checks this token before allowing /inventory/deduct and /inventory/restore
```

---

## Full Request Flow — Customer Places Order

```
1. Customer loads /browse
   GET /menu/items  →  catalog-order :8082
   catalog-order  →  GET /auth/validate  →  auth :8081  ✓

2. Customer clicks "Place Order"
   POST /orders  →  catalog-order :8082
     ├─ validates JWT (auth-service)
     ├─ looks up each MenuItem, verifies restaurantId
     ├─ snapshots prices → creates Order + OrderItems (PENDING)
     └─ POST /inventory/deduct  →  inventory :8083
          ├─ deducts quantity from InventoryItem
          └─ SurplusDetectionService checks quantity vs threshold
               └─ if quantity < threshold → SurplusAlert (notified=false)

3. notification-worker fires (next 60s poll)
   → finds new SurplusAlert
   → fetches subscribers for that restaurant
   → sends email via Mailhog
   → marks alert notified=true

4. Owner sees alert on /surplus (auto-refreshes every 30s)
   GET /inventory/surplus  →  inventory :8083
```

---

## Port Quick Reference

| What | URL |
|---|---|
| Frontend | http://localhost:3000 |
| auth-service | http://localhost:8081 |
| catalog-order-service | http://localhost:8082 |
| inventory-app | http://localhost:8083 |
| Mailhog UI (emails) | http://localhost:8025 |
| auth Postgres | localhost:5433 |
| catalog Postgres | localhost:5434 |
| inventory Postgres | localhost:5435 |
| auth Redis | localhost:6380 |
| catalog Redis | localhost:6381 |
| inventory Redis | localhost:6382 |
