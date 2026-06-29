# Backstage Docker Deployment

This doc covers how to run the Backstage developer portal in Docker with three separate containers: PostgreSQL, the backend API, and the frontend (nginx).

---

## Overview

When you run `yarn start` locally, the backend serves both the API and the React frontend. In Docker we split these into separate containers so they can scale and be updated independently.

```
Your browser
  |
  |-- http://localhost:3000  -->  [backstage-frontend]   nginx, serves static React files
  |
  |-- http://localhost:7007  -->  [backstage-backend]    Node.js, serves the API
                                        |
                                        +-- postgres:5432  -->  [postgres]  PostgreSQL 16
```

The browser talks directly to both the frontend (port 3000) and backend (port 7007). nginx does not proxy API calls — it only serves the built JavaScript files.

---

## What Changed in the Code

These changes were needed to support separate containers. You should know what they are so you understand why things work the way they do.

### 1. Backend no longer serves the frontend

**File changed:** `packages/backend/src/index.ts`

Removed this line:
```ts
backend.add(import('@backstage/plugin-app-backend'));
```

Previously the backend bundled and served the React app. Now nginx does that job. The backend only handles API requests.

### 2. Backend no longer depends on the app package

**File changed:** `packages/backend/package.json`

Removed from `dependencies`:
```json
"@backstage/plugin-app-backend": "^0.5.15",
"app": "link:../app"
```

The backend no longer needs a reference to the frontend package since it doesn't serve it.

### 3. Production config updated for Docker

**File changed:** `app-config.production.yaml`

Key changes:

| Setting | Before | After | Why |
|---|---|---|---|
| `app.baseUrl` | `http://localhost:7007` | `http://localhost:3000` | Frontend is now on port 3000, not served by backend |
| `backend.cors.origin` | not set | `http://localhost:3000` | Browser fetches from :3000 and calls API on :7007, needs CORS |
| `backend.auth.keys` | not set | `${BACKEND_SECRET}` | Required for service-to-service plugin auth tokens |
| `auth.environment` | `development` | `production` | Activates the `github.production` provider block |
| `auth.providers.guest` | `{}` | `dangerouslyAllowOutsideDevelopment: true` | Guest provider is blocked in production env by default |
| `auth.providers.github` | `~` (null) | full `production` block | Needed when `auth.environment: production` |

---

## Files Created

### `packages/app/Dockerfile`

Builds the React frontend in three stages inside Docker:

1. **packages stage** — strips everything except `package.json` files to create a stable install cache layer
2. **build stage** — runs `yarn install` then `yarn workspace app build` to produce `packages/app/dist/`
3. **nginx stage** — copies the built files into `nginx:1.27-alpine` and serves on port 3000

The frontend URLs are baked into the JavaScript bundle at build time using these build args:
- `APP_BASE_URL` (default `http://localhost:3000`)
- `BACKEND_BASE_URL` (default `http://localhost:7007`)

If you move to a different server, update these in `.env` and rebuild.

### `packages/app/nginx.conf`

Configures nginx to:
- Listen on port 3000
- Serve `index.html` for all routes (needed for React Router — without this, refreshing a page gives 404)
- Cache static JS/CSS files for 1 year (they have content hashes in filenames)
- Never cache `index.html` itself so new deploys are picked up immediately
- Add basic security headers (X-Frame-Options, X-Content-Type-Options)

### `packages/backend/Dockerfile.dockerignore`

Tells Docker what to exclude when building the backend image. Excludes `packages/*/src` because the backend uses the **host build** approach — source code is compiled on the host first, only the compiled `dist/` artifacts are copied into the image.

### `packages/app/Dockerfile.dockerignore`

Tells Docker what to exclude when building the frontend image. Keeps `packages/app/src` (needed for the build) but excludes `packages/backend/src` and any node_modules (installed fresh inside Docker).

These are separate from the root `.dockerignore`. Docker BuildKit automatically picks up `<Dockerfile>.dockerignore` next to each Dockerfile, so both builds get their own rules.

### `docker-compose.yml`

Defines three services:

**postgres**
- Image: `postgres:16-alpine`
- Stores data in a named volume `postgres_data` (survives container restarts)
- Has a healthcheck so the backend waits for it to be ready before starting

**backstage-backend**
- Built from `packages/backend/Dockerfile` using the repo root as context
- Exposes port 7007
- Gets database credentials and secrets from environment variables (`.env` file)
- Waits for `postgres` to pass its healthcheck

**backstage-frontend**
- Built from `packages/app/Dockerfile` using the repo root as context
- Exposes port 3000
- Receives `APP_BASE_URL` and `BACKEND_BASE_URL` as build args (baked into JS at build time)

All three services are on a private Docker network called `backstage`. The backend connects to postgres using the service name `postgres` as the hostname (Docker's internal DNS resolves it automatically).

### `scripts/build.sh`

A single script that runs the full build pipeline in the correct order:

```
yarn install --immutable
     ↓
yarn tsc                        (compiles TypeScript → dist-types/)
     ↓
yarn build:backend              (bundles backend → packages/backend/dist/)
     ↓
docker compose up --build -d    (builds images, starts all 3 containers)
```

The backend uses the **host build** approach from the official Backstage docs. This means the TypeScript compilation and bundling happen on your machine (faster, better caching), and Docker only copies the already-built artifacts. The frontend build happens inside Docker automatically.

### `.env.example`

A template for the `.env` file. Copy this to `.env` and fill in your values. The `.env` file is never committed to git.

---

## First-Time Setup

**Step 1 — Create your `.env` file**

```bash
cp .env.example .env
```

**Step 2 — Set a real `BACKEND_SECRET`**

```bash
openssl rand -base64 32
```

Paste the output as the value of `BACKEND_SECRET` in `.env`.

**Step 3 — Set up GitHub OAuth (if you want GitHub sign-in)**

Go to GitHub → Settings → Developer settings → OAuth Apps → New OAuth App

Set these values:
- Homepage URL: `http://localhost:3000`
- Authorization callback URL: `http://localhost:7007/api/auth/github/handler/frame`

Paste the Client ID and Client Secret into `.env`.

**Step 4 — Build and start**

```bash
cd /home/ubuntu/initial-app
yarn install       # needed once after the package.json changes above
./scripts/build.sh
```

---

## Deploying After Every Change

For any code change, run:

```bash
./scripts/build.sh
```

This handles everything — dependency sync, TypeScript compile, backend bundle, Docker image builds, and container restarts.

If you only changed frontend code and want a faster cycle:

```bash
docker compose up --build -d backstage-frontend
```

If you only changed backend code:

```bash
yarn tsc && yarn build:backend
docker compose up --build -d backstage-backend
```

---

## Auth in Docker Explained

### Why `auth.environment: production`?

Backstage auth providers have separate config blocks for `development` and `production` environments. The base `app-config.yaml` sets `auth.environment: development`, which means provider secrets are read from the `github.development:` block.

In Docker we override this to `production` in `app-config.production.yaml`. This means secrets are read from the `github.production:` block — which is also where we place the env var references (`${AUTH_GITHUB_CLIENT_ID}`, etc.).

### Why does guest auth need `dangerouslyAllowOutsideDevelopment`?

The guest auth provider is disabled by default when `auth.environment` is `production` (Backstage blocks it as a safety measure for real deployments). Setting `dangerouslyAllowOutsideDevelopment: true` re-enables it. This is fine for internal tooling or if you want a fallback sign-in method during setup.

### Guest-only sign-in page

The current sign-in page (`packages/app/src/modules/signIn/index.tsx`) only shows the GitHub sign-in button. If you want to also offer guest sign-in without configuring GitHub OAuth, update it:

```tsx
// packages/app/src/modules/signIn/index.tsx
loader: async () => props =>
  React.createElement(SignInPage, {
    ...props,
    providers: [
      'guest',
      {
        id: 'github-auth-provider',
        title: 'GitHub',
        message: 'Sign in with GitHub',
        apiRef: githubAuthApiRef,
      },
    ],
  }),
```

After changing this, run `./scripts/build.sh` so the frontend image is rebuilt.

---

## Useful Commands

```bash
# Check what's running
docker compose ps

# Stream all logs
docker compose logs -f

# Stream logs for one service
docker compose logs -f backstage-backend

# Stop everything (data is kept)
docker compose down

# Stop and delete the database volume (full reset)
docker compose down -v

# Open a PostgreSQL shell
docker compose exec postgres psql -U backstage backstage

# Force a clean rebuild with no cache
DOCKER_BUILDKIT=1 docker compose build --no-cache --progress=plain
docker compose up -d
```

---

## Deploying to a Remote Server

When running on a real server instead of localhost:

1. Update `.env` on the server:
   ```
   APP_BASE_URL=https://portal.yourcompany.com
   BACKEND_BASE_URL=https://api.yourcompany.com
   ```

2. Update the GitHub OAuth App callback URL to match `BACKEND_BASE_URL`:
   ```
   https://api.yourcompany.com/api/auth/github/handler/frame
   ```

3. Run `./scripts/build.sh` — the frontend image must be rebuilt because the URLs are baked in at build time.

4. Add a reverse proxy (nginx, Caddy, etc.) in front of both containers to handle TLS.

---

## Troubleshooting

**Backend crashes immediately after start**
Check that `postgres` is healthy first:
```bash
docker compose logs postgres
docker compose ps
```

**Frontend loads but API calls fail (network error in browser console)**
The `BACKEND_BASE_URL` might not match where the backend is actually accessible from your browser. Check `.env`, then rebuild the frontend image.

**"Unknown provider" error on sign-in**
The `auth.environment` in `app-config.production.yaml` is `production` but the GitHub provider config might be missing or under the wrong key (`development` instead of `production`). Verify `app-config.production.yaml` has `github.production:`.

**TypeScript errors blocking the build**
```bash
yarn tsc --skipLibCheck
```

**"Permission denied" during docker build**
Make sure Docker BuildKit is enabled:
```bash
DOCKER_BUILDKIT=1 docker compose build
```

---

## FoodNest Services in the Backstage Catalog

All five FoodNest microservices are registered in the Backstage Software Catalog with full TechDocs support. Here is what is configured and how it works.

### Services Registered

| Service | Kind | Type | Docs pages |
|---|---|---|---|
| **foodnest** (system) | System | — | system-level grouping |
| **auth-service** | Component | service | 4 (Overview, API, Security, Configuration) |
| **catalog-order-service** | Component | service | 4 (Overview, API, Order Lifecycle, Configuration) |
| **foodnest-frontend** | Component | website | 3 (Overview, Architecture, Authentication Flow) |
| **inventory-app** | Component | service | 4 (Overview, API, Surplus Detection, Configuration) |
| **notification-worker** | Component | service | 3 (Overview, Email Dispatch, Configuration) |

Each service also exposes its API in the Backstage API catalog (auth-api, catalog-order-api, inventory-api) with inline OpenAPI definitions.

### How the catalog is wired

**Local dev (`yarn start`)**

Catalog locations are in `app-config.yaml`. The backend runs from `packages/backend/`, so FoodNest paths go three levels up:

```yaml
# app-config.yaml
catalog:
  locations:
    - type: file
      target: ../../../FoodNest/foodnest-system.yaml
    - type: file
      target: ../../../FoodNest/auth-service/catalog-info.yaml
    # ... and so on for each service
```

**Docker (`docker compose up`)**

The docker-compose.yml mounts the FoodNest directory read-only into the backend container:

```yaml
volumes:
  - ../FoodNest:/app/foodnest:ro
```

The production config references those files at the container path:

```yaml
# app-config.production.yaml
catalog:
  locations:
    - type: file
      target: ./foodnest/foodnest-system.yaml
    - type: file
      target: ./foodnest/auth-service/catalog-info.yaml
    # ... and so on
```

### TechDocs

Each service has:
- A `mkdocs.yml` with `plugins: [techdocs-core]`
- A `docs/` folder with markdown files
- `backstage.io/techdocs-ref: dir:.` in its `catalog-info.yaml`

`dir:.` tells Backstage to look for the `mkdocs.yml` in the same directory as the catalog file.

The base config uses `techdocs.generator.runIn: docker`, which runs the mkdocs build inside a separate Docker container. In a plain Docker Compose environment without Docker-in-Docker, change this to `local` and ensure `mkdocs` and `mkdocs-techdocs-core` are installed on the host:

```bash
pip install mkdocs-techdocs-core
```

Then in `app-config.yaml`:
```yaml
techdocs:
  builder: 'local'
  generator:
    runIn: 'local'
```

### Viewing the catalog

Once the backend starts, navigate to:

- **Catalog home**: http://localhost:7007/catalog (or http://localhost:3000 when using the frontend container)
- **FoodNest system**: http://localhost:3000/catalog/default/system/foodnest
- **TechDocs for auth-service**: http://localhost:3000/docs/default/component/auth-service

All five services will appear under the **foodnest** system in the System view, with their dependency graph showing which services depend on which.

### Adding a new service to the catalog

1. Create `catalog-info.yaml` in the service root following the same pattern as existing services
2. Create `mkdocs.yml` and a `docs/` folder with at least `index.md`
3. Add a `backstage.io/techdocs-ref: dir:.` annotation
4. Add the path to both `app-config.yaml` (local dev) and `app-config.production.yaml` (Docker)
