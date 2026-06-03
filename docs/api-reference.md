# FoodWise — API Reference

All services return JSON. All authenticated endpoints require:
```
Authorization: Bearer <access_token>
```

---

## Auth Service — `http://localhost:8081`

### POST /auth/register

Register a new user.

**Request**
```json
{
  "username": "jane_owner",
  "email": "jane@example.com",
  "password": "SecurePass123!",
  "role": "RESTAURANT_OWNER",
  "restaurantId": 42
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `username` | string | ✅ | 3–50 chars, alphanumeric + `_-` |
| `email` | string | ✅ | Valid email |
| `password` | string | ✅ | 8–128 chars |
| `role` | string | ✅ | `CUSTOMER` \| `RESTAURANT_OWNER` |
| `restaurantId` | number | Owners only | Required if role is `RESTAURANT_OWNER` |

**Response 201**
```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "userId": 1,
  "username": "jane_owner",
  "roles": ["RESTAURANT_OWNER"]
}
```

**Errors**: `400` (validation), `409` (username/email taken)

---

### POST /auth/login

**Request**
```json
{ "usernameOrEmail": "jane@example.com", "password": "SecurePass123!" }
```

**Response 200** — same as register response

**Errors**: `401` (invalid credentials — generic message), `429` (rate limited)

---

### POST /auth/logout

Requires: `Authorization: Bearer <access_token>`

**Response 200**
```json
{ "message": "Logged out successfully" }
```

---

### POST /auth/refresh

**Request**
```json
{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }
```

**Response 200** — new token pair (old refresh token is invalidated)

**Errors**: `401` (invalid/expired/revoked refresh token)

---

### GET /auth/validate

Used by other microservices to validate a JWT.

Requires: `Authorization: Bearer <access_token>`

**Response 200**
```json
{
  "valid": true,
  "userId": 1,
  "username": "jane_owner",
  "email": "jane@example.com",
  "roles": ["RESTAURANT_OWNER"],
  "restaurantId": 42
}
```

If token is invalid: `{ "valid": false }`

---

## Catalog-Order Service — `http://localhost:8082`

### GET /menu/items

Public endpoint. Returns paginated menu items.

**Query params**
| Param | Type | Description |
|-------|------|-------------|
| `restaurantId` | number | Filter by restaurant |
| `page` | number | Page number (0-based) |
| `size` | number | Page size (default 20) |

**Response 200**
```json
{
  "content": [
    {
      "id": 1,
      "restaurantId": 42,
      "name": "Grilled Salmon",
      "description": "Fresh Atlantic salmon",
      "price": 18.99,
      "category": "Mains",
      "available": true,
      "imageUrl": "https://...",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalElements": 25,
  "totalPages": 2,
  "size": 20,
  "number": 0
}
```

---

### POST /menu/items

Requires: `RESTAURANT_OWNER` role

**Request**
```json
{
  "name": "Grilled Salmon",
  "description": "Fresh Atlantic salmon fillet",
  "price": 18.99,
  "category": "Mains",
  "imageUrl": "https://example.com/salmon.jpg"
}
```

**Response 201** — `MenuItemDto`

**Errors**: `400` (validation), `403` (not owner)

---

### PUT /menu/items/{id}

Requires: `RESTAURANT_OWNER` role. Owner can only update their own restaurant's items.

**Response 200** — updated `MenuItemDto`

**Errors**: `404` (not found or not owner's item)

---

### DELETE /menu/items/{id}

Requires: `RESTAURANT_OWNER` role.

**Response 204**

---

### POST /orders

Requires: `CUSTOMER` role

**Request**
```json
{
  "restaurantId": 42,
  "notes": "No onions please",
  "items": [
    { "menuItemId": 1, "quantity": 2 },
    { "menuItemId": 3, "quantity": 1 }
  ]
}
```

| Field | Validation |
|-------|-----------|
| `items` | 1–50 items |
| `quantity` | 1–100 per item |
| All items | Must belong to same restaurant |

**Response 201** — `OrderDto`

**Errors**: `400` (cross-restaurant items), `404` (menu item not found), `409` (item unavailable)

---

### GET /orders/{id}

Requires authentication. Customer sees own orders; owner sees their restaurant's orders.

**Response 200**
```json
{
  "id": 101,
  "customerId": 5,
  "restaurantId": 42,
  "status": "PENDING",
  "totalAmount": 56.97,
  "notes": "No onions please",
  "items": [
    {
      "id": 1,
      "menuItemId": 1,
      "menuItemName": "Grilled Salmon",
      "quantity": 2,
      "unitPrice": 18.99,
      "subtotal": 37.98
    }
  ],
  "createdAt": "2024-01-15T18:45:00Z",
  "updatedAt": "2024-01-15T18:45:00Z"
}
```

---

### PATCH /orders/{id}/status

Requires: `RESTAURANT_OWNER` role

**Request**
```json
{ "status": "CONFIRMED" }
```

**State Machine**
```
PENDING → CONFIRMED | CANCELLED
CONFIRMED → PREPARING | CANCELLED
PREPARING → READY
READY → DELIVERED
DELIVERED → (terminal)
CANCELLED → (terminal)
```

**Errors**: `422` (invalid state transition)

---

### GET /orders/restaurant/{restaurantId}

Requires: `RESTAURANT_OWNER` role. Owners can only view their own restaurant.

Returns paginated `OrderDto` list, ordered by `createdAt DESC`.

---

### GET /orders/customer/{customerId}

Requires authentication. Customers can only view their own orders.

Returns paginated `OrderDto` list.

---

## Inventory Service — `http://localhost:8083`

### GET /inventory/items

Requires: `RESTAURANT_OWNER` role

Returns all inventory items for the authenticated owner's restaurant.

**Response 200** — `InventoryItemDto[]`
```json
[
  {
    "id": 1,
    "restaurantId": 42,
    "menuItemId": 1,
    "name": "Grilled Salmon",
    "quantity": 8,
    "unit": "portions",
    "threshold": 10,
    "expiryDate": "2024-01-16",
    "surplus": true,
    "createdAt": "2024-01-15T10:00:00Z"
  }
]
```

---

### POST /inventory/items

Requires: `RESTAURANT_OWNER` role

**Request**
```json
{
  "menuItemId": 1,
  "name": "Grilled Salmon",
  "quantity": 15,
  "unit": "portions",
  "threshold": 10,
  "expiryDate": "2024-01-20"
}
```

**Response 201** — `InventoryItemDto`

**Errors**: `409` (already exists for this menu item)

---

### PUT /inventory/items/{id}

Requires: `RESTAURANT_OWNER` role

Updates quantity, threshold, expiry date. Triggers surplus check.

**Response 200** — updated `InventoryItemDto`

---

### GET /inventory/surplus

Requires: `RESTAURANT_OWNER` role

Returns items where `quantity < threshold` OR `expiryDate <= now + surplusExpiryHours`.

**Response 200** — `InventoryItemDto[]`

---

### POST /inventory/deduct ⚙️ Internal

**Protected by INTERNAL_SERVICE_TOKEN (not user JWT)**

Called by catalog-order-service after an order is confirmed.

**Request**
```json
{ "menuItemId": 1, "quantity": 2 }
```

**Response 200**

---

### POST /inventory/restore ⚙️ Internal

**Protected by INTERNAL_SERVICE_TOKEN**

Called when an order is cancelled.

**Request**
```json
{ "menuItemId": 1, "quantity": 2 }
```

**Response 200**

---

### POST /notifications/subscribe

Requires: `CUSTOMER` role

Subscribe to surplus alerts from a restaurant.

**Request**
```json
{ "restaurantId": 42 }
```

**Response 201**
```json
{ "message": "Successfully subscribed to surplus alerts" }
```

**Errors**: `409` (already subscribed)

---

### GET /notifications/history

Requires: `CUSTOMER` role

Returns the customer's active subscriptions.

**Response 200** — `NotificationSubscription[]`

---

## Common Error Format

All services return the same error envelope:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2024-01-15T18:45:00Z",
  "path": "/menu/items",
  "fieldErrors": [
    "Price must be non-negative",
    "Name is required"
  ]
}
```

> **Security note**: Error messages are intentionally generic for auth endpoints to prevent user enumeration. Stack traces are never returned.
