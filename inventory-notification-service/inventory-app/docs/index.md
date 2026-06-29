# Inventory Service

The **Inventory Service** manages per-restaurant stock levels, detects surplus conditions, and manages customer subscriptions to surplus alerts. It also exposes internal endpoints consumed by the `catalog-order-service` when orders are placed or cancelled.

## Responsibilities

- **Inventory CRUD** — restaurant owners create and update inventory items with quantities, expiry dates, and low-stock thresholds
- **Surplus detection** — identifies items that are low in stock or nearing expiry and creates `SurplusAlert` records
- **Customer subscriptions** — customers subscribe to receive email notifications when a restaurant has surplus
- **Internal stock operations** — deducts and restores stock on behalf of the order service

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Database | PostgreSQL (`inventory_db`, port 5435) — shared with `notification-worker` |
| Cache | Redis (port 6382) |
| Migrations | Flyway |

## Port

**8083**

## Database Schema

| Table | Purpose |
|-------|---------|
| `inventory_items` | Stock items with quantity, expiry, low-stock threshold |
| `surplus_alerts` | Detected surplus events (consumed by notification-worker) |
| `notification_subscriptions` | Customer email subscriptions per restaurant |
| `notification_logs` | Audit log of sent notifications |

## Surplus Detection Triggers

Surplus detection runs:

1. **On every stock mutation** — after a deduct or restore operation
2. **Hourly scheduled scan** — catches items that transitioned to surplus passively (e.g. expiry approaching over time)

## Related Pages

- [API Reference](api.md)
- [Surplus Detection](surplus-detection.md)
- [Configuration](configuration.md)
