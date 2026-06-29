# API Reference — Auth Service

Base URL: `http://localhost:8081`

---

## POST /auth/register

Register a new user account.

**Auth:** Public

**Request Body**
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "Secret123!",
  "role": "CUSTOMER"
}
```

`role` must be one of: `CUSTOMER`, `RESTAURANT_OWNER`, `ADMIN`

**Response 201**
```json
{
  "message": "User registered successfully",
  "userId": "42"
}
```

**Errors**
- `400` — Validation failure (missing fields, weak password)
- `409` — Username or email already taken
- `429` — Rate limit exceeded (3 registrations / hour per IP)

---

## POST /auth/login

Authenticate and receive a JWT access + refresh token pair.

**Auth:** Public

**Request Body**
```json
{
  "usernameOrEmail": "alice",
  "password": "Secret123!"
}
```

**Response 200**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "d4f9e1c2...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Errors**
- `401` — Invalid credentials (generic message — no enumeration)
- `429` — Rate limit exceeded (5 login attempts / 60s per IP)

---

## POST /auth/logout

Revoke the current session. The access token's JTI is added to the Redis blacklist; the refresh token is marked revoked in the database.

**Auth:** `Bearer <accessToken>`

**Response 204** — No content

**Errors**
- `401` — Missing or invalid token

---

## POST /auth/refresh

Rotate the refresh token and receive a new access + refresh pair.

**Auth:** Public (refresh token passed in body)

**Request Body**
```json
{
  "refreshToken": "d4f9e1c2..."
}
```

**Response 200** — Same shape as `/auth/login`

**Errors**
- `401` — Refresh token expired, revoked, or not found

---

## GET /auth/validate

Internal endpoint. Validates a JWT and returns the user's identity claims. Called by `catalog-order-service` and `inventory-app` on every protected request.

**Auth:** `Bearer <accessToken>`

**Response 200**
```json
{
  "userId": "42",
  "username": "alice",
  "roles": ["CUSTOMER"],
  "restaurantId": null
}
```

**Errors**
- `401` — Token invalid, expired, or blacklisted
