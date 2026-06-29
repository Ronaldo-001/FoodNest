# Catalog & Order Service

The **Catalog & Order Service** is the core business logic service of FoodNest. It manages the restaurant menu catalog and the full lifecycle of customer orders.

## Responsibilities

- **Menu catalog** — restaurant owners create and manage menu items; customers browse them
- **Order management** — customers place orders; restaurant owners progress them through status stages
- **Inventory coordination** — deducts stock from the inventory service when an order is placed; restores stock on cancellation
- **JWT validation** — every protected request forwards the user's token to the auth service for validation

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Database | PostgreSQL (`catalog_db`, port 5434) |
| Cache | Redis (port 6381, menu item caching, 5-min TTL) |
| HTTP Client | Spring WebFlux `WebClient` (async, non-blocking) |
| Migrations | Flyway |

## Port

**8082**

## Database Schema

| Table | Purpose |
|-------|---------|
| `menu_items` | Restaurant menu items with name, price, description, category |
| `orders` | Order records with status, customer, restaurant, total price |
| `order_items` | Line items with **price snapshot** (preserves historical pricing) |

## Service Dependencies

| Dependency | Usage |
|-----------|-------|
| `auth-service:8081` | Validate user JWTs on every protected endpoint |
| `inventory-app:8083` | Deduct/restore stock on order creation/cancellation |

## Inter-Service Call Resilience

Inventory deduction is **best-effort** — if the inventory service is unreachable, the order still succeeds. The order will be flagged and inventory reconciliation can be handled out-of-band.

## Quick Start

```bash
docker compose up catalog-order-service
```

## Related Pages

- [API Reference](api.md)
- [Order Lifecycle](order-lifecycle.md)
- [Configuration](configuration.md)
