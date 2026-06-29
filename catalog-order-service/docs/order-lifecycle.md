# Order Lifecycle

## Status Flow

```
PENDING → CONFIRMED → PREPARING → READY → DELIVERED
    └──────────────────────────────────────→ CANCELLED
```

All states except `DELIVERED` can transition to `CANCELLED`. Any other transition not shown above returns `400 Bad Request`.

## Status Descriptions

| Status | Description |
|--------|-------------|
| `PENDING` | Order placed by customer; awaiting restaurant confirmation |
| `CONFIRMED` | Restaurant accepted the order |
| `PREPARING` | Kitchen is preparing the order |
| `READY` | Order is ready for pickup / dispatch |
| `DELIVERED` | Order delivered to customer — terminal state |
| `CANCELLED` | Order cancelled — terminal state; triggers inventory restore |

## Inventory Interaction

| Event | Action |
|-------|--------|
| Order placed (`PENDING`) | `POST /inventory/deduct` called on `inventory-app` with INTERNAL_SERVICE_TOKEN |
| Order cancelled | `POST /inventory/restore` called to return stock |
| Order delivered | No inventory action |

Inventory calls are best-effort. A network failure will not prevent an order from being created or cancelled, but inventory counts may drift and require manual reconciliation.

## Price Snapshots

At order creation, the current price of each menu item is captured in `order_items.unit_price`. This prevents historical orders from showing wrong prices if a menu item's price is later updated.

## Who Can Transition Status

Only `RESTAURANT_OWNER` users can call `PATCH /orders/{id}/status`. The owner must belong to the same restaurant as the order.

Customers cannot modify order status after placement. They may request cancellation through a separate flow (if implemented).

## Transition Validation Example

```
PENDING → DELIVERED  →  400 Bad Request (must pass through intermediate states)
DELIVERED → CANCELLED  →  400 Bad Request (terminal state, cannot transition)
CONFIRMED → CANCELLED  →  200 OK
```
