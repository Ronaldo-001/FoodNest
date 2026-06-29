# Security — Auth Service

## JWT Configuration

| Property | Value |
|----------|-------|
| Algorithm | HS256 |
| Access token TTL | 15 minutes (configurable) |
| Refresh token TTL | 7 days (configurable) |
| JTI claim | Unique per token (used for blacklisting) |

Access tokens are short-lived so a blacklist lookup in Redis is only needed during the token's remaining valid window after logout.

## Password Security

- Hashing: **BCrypt** with strength 10
- Minimum requirements enforced at registration
- Passwords are never stored in plain text or logged

## Token Blacklist

On logout, the token's `jti` (JWT ID) is written to Redis with a TTL matching the token's remaining validity. All subsequent calls to `/auth/validate` check the blacklist before accepting the token. This provides immediate revocation without requiring a stateful session.

## Rate Limiting

Rate limits are enforced per client IP address using Redis counters with sliding windows.

| Endpoint | Limit |
|----------|-------|
| `POST /auth/login` | 5 requests / 60 seconds |
| `POST /auth/register` | 3 requests / 1 hour |

Exceeding the limit returns `429 Too Many Requests`.

## User Enumeration Prevention

The login endpoint returns the same generic error message regardless of whether the username does not exist or the password is wrong. This prevents attackers from discovering valid usernames by probing the API.

## Security Headers

All responses include:

| Header | Value |
|--------|-------|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Referrer-Policy` | `no-referrer` |

## CORS

Allowed origins are controlled via the `CORS_ALLOWED_ORIGINS` environment variable. In development this defaults to `http://localhost:3000`. In production, restrict this to your actual frontend domain.

## Internal Service Calls

Other FoodNest services call `GET /auth/validate` to validate user JWTs. These calls are service-to-service and do not traverse a public network boundary in a Docker or Kubernetes deployment.
