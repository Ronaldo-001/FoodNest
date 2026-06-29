# Auth Service

The **Auth Service** is the central authentication and authorization provider for the FoodNest platform. Every protected request across the platform is validated through this service.

## Responsibilities

- User registration with role assignment (`CUSTOMER`, `RESTAURANT_OWNER`, `ADMIN`)
- Login with username/email and password; issues JWT access and refresh token pairs
- Logout with token blacklisting (prevents reuse of revoked tokens)
- Refresh token rotation — exchanges a valid refresh token for a new access + refresh pair
- Internal JWT validation endpoint consumed by all other FoodNest services

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JJWT 0.12.3 |
| Database | PostgreSQL (`auth_db`, port 5433) |
| Cache / Blacklist | Redis (port 6380) |
| Migrations | Flyway |

## Port

**8081**

## Database Schema

| Table | Purpose |
|-------|---------|
| `users` | User accounts (username, email, bcrypt password hash, role) |
| `roles` | Role definitions |
| `refresh_tokens` | Active refresh tokens with expiry and revocation flag |

## Dependencies

None — the auth service has no runtime dependencies on other FoodNest services. It is the root of the trust chain.

## Quick Start

```bash
# With docker compose
docker compose up auth-service

# Direct (requires Postgres + Redis running)
cd auth-service
./mvnw spring-boot:run
```

## Related Pages

- [API Reference](api.md)
- [Security](security.md)
- [Configuration](configuration.md)
