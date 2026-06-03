# Auth Service — FoodWise

Authentication and authorization microservice. Issues JWTs, manages refresh tokens, and provides `/auth/validate` for other services.

**Port**: `8081`  
**Package**: `com.foodwise.auth`

---

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | Public | Register a new user |
| POST | `/auth/login` | Public | Login and receive JWT |
| POST | `/auth/logout` | Bearer | Revoke tokens and blacklist current JWT |
| POST | `/auth/refresh` | Public | Rotate refresh token |
| GET | `/auth/validate` | Bearer | **Internal** — validate JWT for other services |

---

## Request/Response Examples

### POST /auth/register
```json
// Request
{
  "username": "jane_owner",
  "email": "jane@example.com",
  "password": "SecurePass123!",
  "role": "RESTAURANT_OWNER",
  "restaurantId": 42
}

// Response 201
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshToken": "uuid-string",
  "userId": 1,
  "username": "jane_owner",
  "roles": ["RESTAURANT_OWNER"]
}
```

### POST /auth/login
```json
// Request
{ "usernameOrEmail": "jane@example.com", "password": "SecurePass123!" }
// Response 200 — same as register response
```

### GET /auth/validate
```
Authorization: Bearer <access_token>
// Response 200
{
  "valid": true,
  "userId": 1,
  "username": "jane_owner",
  "email": "jane@example.com",
  "roles": ["RESTAURANT_OWNER"],
  "restaurantId": 42
}
```

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `JWT_SECRET` | ✅ Yes | ≥ 32-char random secret |
| `POSTGRES_HOST` | ✅ Yes | PostgreSQL host |
| `POSTGRES_USER` | ✅ Yes | DB username |
| `POSTGRES_PASSWORD` | ✅ Yes | DB password |
| `REDIS_HOST` | ✅ Yes | Redis host |
| `CORS_ALLOWED_ORIGINS` | No | Defaults to `http://localhost:3000` |

Generate a secret: `openssl rand -hex 32`

---

## Running Locally

```bash
# From project root
docker compose up auth-postgres auth-redis auth-service
```

Service will be available at http://localhost:8081

---

## Security Notes

- JWT uses HS256, `exp` claim enforced, `none` algorithm rejected
- BCrypt password hashing (strength 10)
- Rate limiting: 5 login attempts / 60s, 3 registrations / 1h (per IP)
- Token blacklist in Redis on logout (fail-closed if Redis unavailable)
- Refresh token rotation on every use
- Generic error messages on auth failure (prevents user enumeration)
- TODO(security): Add OAuth 2.0 provider integration
- TODO(security): Add MFA support
- TODO(security): Add leaked password detection
