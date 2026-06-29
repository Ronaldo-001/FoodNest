# Authentication Flow — FoodNest Frontend

## Token Storage Strategy

JWT tokens are stored **in memory only** (React state via `AuthContext`). They are never written to `localStorage` or `sessionStorage`. This is a deliberate security decision to prevent XSS attacks from reading tokens.

**Trade-off:** Tokens are lost on page refresh. Users must log in again after a hard refresh or browser tab close.

## Login Flow

```
User submits form
      ↓
POST /auth/login  →  { accessToken, refreshToken, userId, username, roles, restaurantId }
      ↓
AuthContext.login() stores tokens in React state
      ↓
Axios interceptor sets Authorization: Bearer <accessToken> on all subsequent requests
      ↓
Navigate to /dashboard or /browse based on role
```

## Logout Flow

```
User clicks Logout
      ↓
POST /auth/logout  (sends current accessToken in header)
      ↓
AuthContext.logout() clears state
      ↓
window.location.href = '/login'  (hard redirect — clears all in-memory state)
```

The hard redirect ensures no residual token state remains in any React component.

## Token Refresh

Refresh token rotation is a TODO in the current codebase. Currently, when the access token expires (after 15 minutes), the next API call returns `401` and the user is logged out.

A production implementation would add an Axios response interceptor that:
1. Detects `401` on a non-auth endpoint
2. Calls `POST /auth/refresh` with the in-memory refresh token
3. Updates the access token in `AuthContext` and retries the original request

## AuthContext Shape

```js
{
  user: {
    userId: "42",
    username: "alice",
    roles: ["CUSTOMER"],
    restaurantId: null
  },
  accessToken: "eyJ...",
  refreshToken: "d4f9...",
  login: (data) => void,
  logout: () => void,
  isAuthenticated: boolean
}
```

## Protected Routes

`ProtectedRoute` wraps role-sensitive pages. It reads `isAuthenticated` and `user.roles` from `AuthContext`:

```jsx
<ProtectedRoute requiredRole="RESTAURANT_OWNER">
  <RestaurantDashboard />
</ProtectedRoute>
```

If the user is not authenticated or lacks the required role, they are redirected to `/login`.
