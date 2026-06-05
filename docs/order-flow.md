# Order Flow — Logic & Architecture

## Overview

Orders flow through three services: **frontend → catalog-order-service → inventory-app (for stock deduction)**.
Authentication is validated by **auth-service** on every protected request.

---

## 1. Customer Places an Order

### Frontend (`CustomerBrowse.jsx`)
1. Customer browses menu items (`GET /api/catalog/menu/items`)
2. Customer adds items to cart (client-side state only — no API call)
3. Customer clicks **Place Order** — payload sent:
```json
{
  "restaurantId": 1,
  "notes": "No onions please",
  "items": [
    { "menuItemId": 5, "quantity": 2 },
    { "menuItemId": 8, "quantity": 1 }
  ]
}
```
4. Request: `POST /api/catalog/orders` → nginx proxies to `catalog-order-service:8082/orders`

### Backend (`OrderService.createOrder`)
```
JWT validated → caller must have CUSTOMER role
      ↓
For each item:
  - Menu item exists? (404 if not)
  - Menu item belongs to the given restaurantId? (400 if mismatch)
  - Menu item is available? (409 if not)
  - Calculate subtotal (unitPrice × quantity)
      ↓
Order saved to DB with status = PENDING
      ↓
Inventory deduction called (best-effort, non-fatal):
  POST inventory-app:8083/inventory/deduct per item
  If inventory service is down → order still succeeds, warning logged
      ↓
Returns 201 Created with full OrderDto
```

### Why inventory deduction is non-fatal
The catalog service does not own inventory state. If the inventory service is temporarily unavailable, the order is not rolled back — the restaurant owner reconciles manually. This trades consistency for availability.

---

## 2. Restaurant Sees Incoming Orders

### Where to look
- **Dashboard → Orders tab** — embedded in the restaurant dashboard, auto-refreshes every 30 seconds
- **Orders page** (navbar link) — full-page view of all orders with the same management buttons

### How it fetches
```
GET /api/catalog/orders/restaurant/{restaurantId}
```
- JWT is validated; `restaurantId` from the JWT must match the path param
- Returns paginated list sorted by `created_at DESC` (newest first)
- Pending orders are highlighted with an amber border

---

## 3. Order Status Lifecycle

```
PENDING
  │  Restaurant clicks "Confirm"
  ▼
CONFIRMED
  │  Restaurant clicks "Start Preparing"
  ▼
PREPARING
  │  Restaurant clicks "Mark Ready"
  ▼
READY
  │  Restaurant clicks "Mark Delivered"
  ▼
DELIVERED  (terminal)

At any non-terminal step, "Cancel" is available → CANCELLED (terminal)
```

State transitions are enforced server-side in `OrderService.updateStatus()`.
Invalid transitions (e.g. PENDING → DELIVERED) throw `400 Bad Request`.

When an order is CANCELLED, `InventoryClient.restoreStock()` is called to reverse the deduction (also best-effort).

### API call
```
PATCH /api/catalog/orders/{id}/status
Body: { "status": "CONFIRMED" }
```
Only the restaurant that owns the order (JWT `restaurantId` matches) can update status.
Customers cannot change status — the endpoint checks the caller's role and restaurantId.

---

## 4. JWT & Authentication Flow per Request

Every protected request to catalog or inventory goes through this chain:

```
Browser
  └─ Authorization: Bearer <access_token>
        ↓ nginx proxy_pass
  catalog-order-service:8082
        ↓ JwtAuthenticationFilter
  GET http://auth-service:8081/auth/validate
  (with the same Bearer token)
        ↓ auth-service validates JWT signature + blacklist check + DB user lookup
  Returns ValidateResponse { userId, username, roles, restaurantId, valid }
        ↓
  SecurityContext.Authentication = ValidateResponse
        ↓
  Controller uses (ValidateResponse) authentication.getPrincipal()
```

**Access token lifetime:** 15 minutes. After expiry, the frontend must call `POST /api/auth/refresh` with the refresh token to get a new pair.

---

## 5. Key Identifiers

| Field | Set by | Used for |
|---|---|---|
| `restaurantId` (on User) | Provided at registration by RESTAURANT_OWNER | Menu item ownership, order routing, access control |
| `customerId` (on Order) | `caller.getUserId()` from JWT at order creation | Customer's order history |
| `restaurantId` (on Order) | Taken from `CreateOrderRequest.restaurantId` (from cart item) | Restaurant's order list |

---

## 6. Rebuild After Code Changes

Any change to Java backend code requires a Docker rebuild:

```bash
# Rebuild backend services only
docker-compose up -d --build auth-service catalog-order-service inventory-app

# Rebuild everything including frontend
docker-compose up -d --build

# After rebuilding auth-service: log out and log back in
# The access token is stored in memory — a new login issues a fresh token
# with all updated fields (e.g. restaurantId added to TokenResponse)
```

---

## 7. Common Issues & Fixes

| Symptom | Cause | Fix |
|---|---|---|
| Menu Items stat = 0 on dashboard | `restaurantId` was missing from `TokenResponse` — query disabled | Fixed: `TokenResponse.restaurantId` added; log out and back in |
| Place order: no visible feedback | Status update buttons didn't invalidate query cache | Fixed: all mutations use `useMutation` + `queryClient.invalidateQueries` |
| 403 on register/login | CORS preflight (`OPTIONS`) blocked by `anyRequest().denyAll()` | Fixed: `OPTIONS /**` → `permitAll()` added to all SecurityConfig files |
| JWT_SECRET not set on startup | Shell env var `JWT_SECRET=""` overrides `.env` file | Run `unset JWT_SECRET` then restart containers |
| Auth-service crash on startup | Inline comments in `.env` (e.g. `900000 # 15 minutes`) parsed as value | Fixed: comments removed from `.env.example` |
