# Surplus Detection

## What is Surplus?

A surplus condition occurs when an inventory item meets one of the following criteria:

| Condition | Rule |
|-----------|------|
| `LOW_STOCK` | Item's current quantity is below its configured `lowStockThreshold` |
| `EXPIRY_SOON` | Item's `expiryDate` is within the configured expiry warning window (default: 24 hours) |

## Detection Triggers

Surplus detection runs in two ways:

### 1. On-Demand (after stock mutations)

Triggered automatically after:
- `POST /inventory/deduct` — stock deduction by the order service
- `POST /inventory/restore` — stock restoration on order cancellation
- `PUT /inventory/items/{id}` — manual quantity or threshold update by restaurant owner

### 2. Scheduled Scan (hourly)

A Spring `@Scheduled` job scans all inventory items every hour. This catches items that become surplus passively — for example, an item whose expiry date crosses into the warning window while no mutations are occurring.

## Alert Creation

When a surplus condition is detected:

1. A `SurplusAlert` record is written to the `surplus_alerts` table with:
   - `itemId`, `restaurantId`
   - `reason` (`LOW_STOCK` or `EXPIRY_SOON`)
   - `detectedAt` timestamp
   - `notified = false`

2. The `notification-worker` polls this table every 60 seconds, finds `notified = false` records, and dispatches emails to subscribed customers.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `SURPLUS_QUANTITY_THRESHOLD` | `10` | Default low-stock threshold for new items (can be overridden per item) |
| `SURPLUS_EXPIRY_HOURS` | `24` | Hours before expiry to trigger `EXPIRY_SOON` |

## Example Scenario

A restaurant has Tomatoes with:
- `quantity = 8`
- `lowStockThreshold = 10`
- `expiryDate = tomorrow`

Both `LOW_STOCK` and `EXPIRY_SOON` conditions are true. Two `SurplusAlert` records are created (one per reason). Subscribed customers receive two emails within the next 60-second notification worker poll cycle.
