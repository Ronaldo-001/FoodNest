# API Reference — Catalog & Order Service

Base URL: `http://localhost:8082`

All protected endpoints require `Authorization: Bearer <accessToken>`.

---

## Menu Endpoints

### GET /menu/items

Browse all menu items. Supports filtering and pagination.

**Auth:** Public

**Query Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `restaurantId` | string | Filter by restaurant |
| `category` | string | Filter by category |
| `page` | int | Page number (0-indexed) |
| `size` | int | Page size (default 20) |

**Response 200**
```json
{
  "content": [
    {
      "id": "1",
      "name": "Margherita Pizza",
      "price": 12.99,
      "category": "PIZZA",
      "restaurantId": "5",
      "available": true
    }
  ],
  "totalElements": 42,
  "totalPages": 3
}
```

---

### GET /menu/items/{id}

Get a single menu item by ID.

**Auth:** Public

**Response 200** — single menu item object

**Errors:** `404` — item not found

---

### POST /menu/items

Create a new menu item.

**Auth:** `Bearer` + `RESTAURANT_OWNER` role

**Request Body**
```json
{
  "name": "Margherita Pizza",
  "description": "Classic tomato and mozzarella",
  "price": 12.99,
  "category": "PIZZA"
}
```

**Response 201** — created menu item

**Errors:** `403` — not a restaurant owner

---

### PUT /menu/items/{id}

Update a menu item. Owners can only update their own restaurant's items.

**Auth:** `Bearer` + `RESTAURANT_OWNER`

**Response 200** — updated item

---

### DELETE /menu/items/{id}

Delete a menu item.

**Auth:** `Bearer` + `RESTAURANT_OWNER`

**Response 204**

---

## Order Endpoints

### POST /orders

Place a new order. Triggers inventory stock deduction.

**Auth:** `Bearer` + `CUSTOMER` role

**Request Body**
```json
{
  "restaurantId": "5",
  "items": [
    { "menuItemId": "1", "quantity": 2 },
    { "menuItemId": "3", "quantity": 1 }
  ],
  "deliveryAddress": "123 Main St"
}
```

**Response 201**
```json
{
  "orderId": "88",
  "status": "PENDING",
  "totalPrice": 38.97,
  "createdAt": "2025-06-01T12:00:00Z"
}
```

---

### GET /orders/{id}

Retrieve order details.

**Auth:** `Bearer` — customer (own orders) or restaurant owner

**Response 200** — full order object with items and status history

---

### PATCH /orders/{id}/status

Advance an order through its lifecycle.

**Auth:** `Bearer` + `RESTAURANT_OWNER`

**Request Body**
```json
{ "status": "CONFIRMED" }
```

See [Order Lifecycle](order-lifecycle.md) for valid transitions.

**Errors:** `400` — invalid status transition

---

### GET /orders/restaurant/{restaurantId}

List all orders for a restaurant (paginated).

**Auth:** `Bearer` + `RESTAURANT_OWNER`

---

### GET /orders/customer/{customerId}

List order history for a customer (paginated).

**Auth:** `Bearer` + `CUSTOMER`
