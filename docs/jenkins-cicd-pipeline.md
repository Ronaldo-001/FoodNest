# Jenkins CI/CD Pipeline — FoodNest

This document describes the Jenkins CI/CD pipeline configuration for all five FoodNest microservices, including Trivy security scanning and Docker image builds.

---

## Overview

Each microservice has its own `Jenkinsfile` that defines a four-stage pipeline:

```
Checkout → Trivy Filesystem Scan → Build Docker Image → Trivy Image Scan
```

Security scanning is performed **twice per build**:
- **Before build** — Trivy scans the source code and dependencies (filesystem scan)
- **After build** — Trivy scans the final Docker image for OS/package vulnerabilities

---

## Services & File Locations

| Service | Jenkinsfile | Pipeline YAML | Docker Image |
|---|---|---|---|
| Auth Service | [auth-service/Jenkinsfile](../auth-service/Jenkinsfile) | [jenkins/auth-service-pipeline.yaml](../jenkins/auth-service-pipeline.yaml) | `foodwise/auth-service` |
| Catalog Order Service | [catalog-order-service/Jenkinsfile](../catalog-order-service/Jenkinsfile) | [jenkins/catalog-order-service-pipeline.yaml](../jenkins/catalog-order-service-pipeline.yaml) | `foodwise/catalog-order-service` |
| Inventory App | [inventory-notification-service/inventory-app/Jenkinsfile](../inventory-notification-service/inventory-app/Jenkinsfile) | [jenkins/inventory-app-pipeline.yaml](../jenkins/inventory-app-pipeline.yaml) | `foodwise/inventory-app` |
| Notification Worker | [inventory-notification-service/notification-worker/Jenkinsfile](../inventory-notification-service/notification-worker/Jenkinsfile) | [jenkins/notification-worker-pipeline.yaml](../jenkins/notification-worker-pipeline.yaml) | `foodwise/notification-worker` |
| Frontend | [frontend/Jenkinsfile](../frontend/Jenkinsfile) | [jenkins/frontend-pipeline.yaml](../jenkins/frontend-pipeline.yaml) | `foodwise/frontend` |

---

## Pipeline Stages

### Stage 1 — Checkout

Checks out the full monorepo. Each Jenkinsfile references only its own service subdirectory for subsequent stages.

```groovy
stage('Checkout') {
    steps {
        checkout scm
    }
}
```

### Stage 2 — Install Trivy

Installs [Trivy](https://trivy.dev) (v0.50.1) if not already present on the agent. The version is pinned via the `TRIVY_VERSION` environment variable.

```sh
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh \
    | sh -s -- -b /usr/local/bin v0.50.1
```

### Stage 3 — Trivy Filesystem Scan

Scans the service source directory for HIGH and CRITICAL CVEs in dependencies and config files. Uses `--exit-code 0` so the build continues even if issues are found — issues are reported as warnings.

```sh
trivy fs \
    --exit-code 0 \
    --severity HIGH,CRITICAL \
    --ignore-unfixed \
    --format table \
    --output trivy-fs-report.txt \
    <service-dir>/
```

Output is archived as `trivy-fs-report.txt` in build artifacts.

### Stage 4 — Build Docker Image

Builds the Docker image with two tags: the build number and `latest`.

```sh
docker build \
    -t foodwise/<service>:${BUILD_NUMBER} \
    -t foodwise/<service>:latest \
    <service-dir>/
```

### Stage 5 — Trivy Image Scan

Scans the built Docker image for CRITICAL CVEs. Uses `--exit-code 1` to **fail the build** if any unfixed critical vulnerabilities are found in the final image.

```sh
trivy image \
    --exit-code 1 \
    --severity CRITICAL \
    --ignore-unfixed \
    --format table \
    --output trivy-image-report.txt \
    foodwise/<service>:${BUILD_NUMBER}
```

Output is archived as `trivy-image-report.txt` in build artifacts.

---

## Security Scan Behavior

| Stage | Severity Filter | Exit Code | Effect on Build |
|---|---|---|---|
| Filesystem Scan | HIGH, CRITICAL | 0 | Warning only — build continues |
| Image Scan | CRITICAL only | 1 | Build fails if unfixed CVEs found |

The `--ignore-unfixed` flag excludes vulnerabilities that have no fix available, preventing false-positive failures.

---

## Jenkins Job Registration

Pipeline jobs are registered using [Jenkins Job Builder](https://jenkins-job-builder.readthedocs.io/). YAML configs live in the `jenkins/` directory.

### Prerequisites

```sh
pip install jenkins-job-builder
```

### `jenkins.ini` configuration

Create a `jenkins.ini` file (do not commit credentials):

```ini
[jenkins]
url=http://<your-jenkins-host>:8080
user=<jenkins-user>
password=<jenkins-api-token>
```

### Register all pipelines

```sh
# Register a single pipeline
jenkins-jobs --conf jenkins.ini update jenkins/auth-service-pipeline.yaml

# Register all 5 pipelines at once
jenkins-jobs --conf jenkins.ini update jenkins/
```

Before running, replace `<YOUR_ORG>` in each YAML file with your GitHub organisation/user and ensure a Jenkins credential with ID `github-credentials` exists.

---

## Jenkins Agent Requirements

The Jenkins agent running these pipelines must have:

| Tool | Purpose |
|---|---|
| `docker` CLI + daemon access | Building and scanning images |
| `curl` | Downloading Trivy installer |
| Internet access | Trivy vulnerability DB download |
| `git` | Source checkout |

If Trivy is pre-installed on the agent, the install step is skipped automatically.

---

## Build Artifacts

Each build archives two reports:

- `trivy-fs-report.txt` — filesystem/dependency scan results
- `trivy-image-report.txt` — Docker image layer scan results

These are available in **Build → Artifacts** in the Jenkins UI and are retained for the last 5 builds per job.

---

## Environment Variables

All Jenkinsfiles expose these variables for easy overriding:

| Variable | Default | Description |
|---|---|---|
| `DOCKER_REGISTRY` | `foodwise` | Image name prefix |
| `IMAGE_NAME` | *(per service)* | Service image name |
| `IMAGE_TAG` | `${BUILD_NUMBER}` | Image tag |
| `TRIVY_VERSION` | `0.50.1` | Trivy release to install |

---

## Extending the Pipeline

To add Docker push after a successful scan, add a `Push Image` stage with registry credentials:

```groovy
stage('Push Docker Image') {
    when {
        branch 'main'
    }
    steps {
        withCredentials([usernamePassword(
            credentialsId: 'dockerhub-credentials',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
        )]) {
            sh '''
                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest
            '''
        }
    }
}
```

Add the Jenkins credential `dockerhub-credentials` (Username with password kind) under **Manage Jenkins → Credentials**.
