# FoodNest Frontend

The **FoodNest Frontend** is a React single-page application that serves as the primary user interface for both restaurant owners and customers.

## Responsibilities

- **Authentication** — login, registration, token management
- **Restaurant owner dashboard** — manage menu items, track and update incoming orders, view inventory
- **Customer experience** — browse restaurant menus, place orders, view order history, subscribe to surplus alerts

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Framework | React 18.3.1 |
| Build Tool | Vite 5.2.13 |
| Routing | react-router-dom |
| Server State | @tanstack/react-query |
| HTTP Client | axios |
| Notifications | react-hot-toast |
| Production Server | Nginx (Docker) |

## Port

**3000** (Vite dev server) / **3000** (Nginx in Docker)

## Pages

| Route | Access | Description |
|-------|--------|-------------|
| `/login` | Public | Login form |
| `/register` | Public | Registration (choose CUSTOMER or RESTAURANT_OWNER role) |
| `/dashboard` | RESTAURANT_OWNER | Menu management, inventory view, incoming orders |
| `/browse` | CUSTOMER | Browse menus and place orders |
| `/orders` | All authenticated | Order history |
| `/surplus` | CUSTOMER | Manage surplus alert subscriptions |

## Backend Dependencies

| Service | Purpose |
|---------|---------|
| `auth-service:8081` | Login, register, logout, token refresh |
| `catalog-order-service:8082` | Menu browsing, order placement and tracking |
| `inventory-app:8083` | Inventory view, surplus subscriptions |

## Related Pages

- [Architecture](architecture.md)
- [Authentication Flow](authentication.md)
