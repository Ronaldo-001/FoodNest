# Architecture — FoodNest Frontend

## Project Structure

```
frontend/src/
├── contexts/
│   └── AuthContext.jsx        # Global auth state
├── components/
│   ├── Navbar.jsx             # Navigation with role-based links
│   └── ProtectedRoute.jsx     # Route guard
├── pages/
│   ├── Login.jsx
│   ├── Register.jsx
│   ├── RestaurantDashboard.jsx
│   ├── CustomerBrowse.jsx
│   ├── Orders.jsx
│   └── SurplusAlerts.jsx
├── api/
│   └── axios.js               # Axios instance with Bearer token injection
└── main.jsx
```

## State Management

| Concern | Library |
|---------|---------|
| Auth state (token, user, roles) | React Context (`AuthContext`) |
| Server data (menus, orders, inventory) | `@tanstack/react-query` |
| Toast feedback | `react-hot-toast` |

React Query handles caching and automatic re-fetching. Mutations call `queryClient.invalidateQueries()` to keep the UI in sync after writes.

## API Configuration

Backend URLs are injected at build time via Vite environment variables:

```env
VITE_API_BASE_URL=http://localhost:8081       # auth-service
VITE_CATALOG_API_URL=http://localhost:8082    # catalog-order-service
VITE_INVENTORY_API_URL=http://localhost:8083  # inventory-app
```

Three separate Axios instances are created — one per backend service — each sharing the same Bearer token injection interceptor.

## Role-Based Access Control

Routes and UI elements are conditionally rendered based on the user's role from `AuthContext`:

| UI Element | Visible To |
|-----------|-----------|
| Dashboard link | `RESTAURANT_OWNER` |
| Browse link | `CUSTOMER` |
| Surplus alerts link | `CUSTOMER` |
| Order management actions | `RESTAURANT_OWNER` |
| Place order button | `CUSTOMER` |

Attempting to navigate to a route without the required role redirects to `/login`.

## Error Handling

- Network errors and API error responses surface as `react-hot-toast` notifications
- `401` responses trigger a logout (token expired or revoked)
- React Query's `retry` is disabled for `4xx` errors
