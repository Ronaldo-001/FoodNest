# Contributing to FoodWise

## Development Setup

```bash
git clone <repo-url>
cd FoodNest
cp .env.example .env
# Fill in required values in .env

# Start infrastructure
docker compose up auth-postgres catalog-postgres inventory-postgres \
                  auth-redis catalog-redis inventory-redis mailhog -d

# Run services (each in separate terminal)
cd auth-service && mvn spring-boot:run
cd catalog-order-service && mvn spring-boot:run
cd inventory-notification-service/inventory-app && mvn spring-boot:run
cd inventory-notification-service/notification-worker && mvn spring-boot:run
cd frontend && cp .env.example .env.local && npm install && npm run dev
```

## Branch Strategy

- `main` — stable, production-ready
- `develop` — integration branch
- Feature branches: `feature/<description>`
- Bug fixes: `fix/<description>`

## Commit Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):
```
feat(auth): add OAuth2 Google provider
fix(catalog): prevent negative order quantities
docs(api): add missing /auth/refresh examples
chore(ci): bump Node version to 20
```

## Pull Request Checklist

- [ ] All CI checks pass
- [ ] New endpoints have Bean Validation on request DTOs
- [ ] Business logic is in the service layer, not controllers
- [ ] RBAC ownership checks present for multi-tenant data
- [ ] No secrets committed (use `.env` which is git-ignored)
- [ ] New SQL migrations are versioned sequentially (`V3__...sql`)
- [ ] Error messages on auth endpoints are generic (no user enumeration)

## Code Style

- Java: standard formatting, Lombok for boilerplate
- React: functional components only, hooks for logic
- SQL: `snake_case` columns, `UPPER_CASE` SQL keywords

## Running Tests

```bash
# Java (per service)
mvn test

# Frontend
cd frontend && npm test
```
