# API Reference — Inventory Service

Base URL: `http://localhost:8083`

Protected endpoints require `Authorization: Bearer <accessToken>` (user JWT, validated against auth-service).

Internal endpoints require `Authorization: Bearer <INTERNAL_SERVICE_TOKEN>`.

---

## Inventory Endpoints (RESTAURANT_OWNER)

### GET /inventory/items

List all inventory items for the authenticated owner's restaurant.

**Auth:** `Bearer` + `RESTAURANT_OWNER`

**Response 200**
```json
[
  {
    "id": "10",
    "name": "Tomatoes",
    "quantity": 50,
    "unit": "kg",
    "expiryDate": "2025-06-05",
    "lowStockThreshold": 10,
    "restaurantId": "5"
  }
]
```

---

### POST /inventory/items

Create a new inventory item.

**Auth:** `Bearer` + `RESTAURANT_OWNER`

**Request Body**
```json
{
  "name": "Tomatoes",
  "quantity": 50,
  "unit": "kg",
  "expiryDate": "2025-06-05",
  "lowStockThreshold": 10
}
```

**Response 201** — created item

---

### PUT /inventory/items/{id}

Update an inventory item (quantity, expiry date, threshold, etc.).

**Auth:** `Bearer` + `RESTAURANT_OWNER`

Triggers surplus detection after update.

**Response 200** — updated item

---

### GET /inventory/surplus

Get current surplus/low-stock items for the authenticated restaurant.

**Auth:** `Bearer` + `RESTAURANT_OWNER`

**Response 200**
```json
[
  {
    "itemId": "10",
    "name": "Tomatoes",
    "surplusReason": "LOW_STOCK",
    "quantity": 5,
    "threshold": 10
  }
]
```

---

## Internal Endpoints (INTERNAL_SERVICE_TOKEN)

### POST /inventory/deduct

Deduct stock when an order is placed. Called by `catalog-order-service`.

**Auth:** `Bearer <INTERNAL_SERVICE_TOKEN>`

**Request Body**
```json
{
  "orderId": "88",
  "restaurantId": "5",
  "items": [
    { "itemName": "Tomatoes", "quantity": 2 }
  ]
}
```

**Response 200** — deduction result with updated quantities

---

### POST /inventory/restore

Restore stock when an order is cancelled.

**Auth:** `Bearer <INTERNAL_SERVICE_TOKEN>`

Same request shape as `/inventory/deduct`.

**Response 200**

---

## Notification Subscription Endpoints (CUSTOMER)

### POST /notifications/subscribe

Subscribe to surplus alerts for a restaurant.

**Auth:** `Bearer` + `CUSTOMER`

**Request Body**
```json
{
  "restaurantId": "5",
  "email": "alice@example.com"
}
```

**Response 201**

---

### GET /notifications/history

Get the authenticated customer's current subscriptions.

**Auth:** `Bearer` + `CUSTOMER`

**Response 200**
```json
[
  { "restaurantId": "5", "restaurantName": "Mario's", "subscribedAt": "2025-05-01T10:00:00Z" }
]
```
