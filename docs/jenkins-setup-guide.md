# Jenkins Setup Guide — FoodNest on EC2 (Native Install)

Complete walkthrough: fresh EC2 instance → Jenkins installed as a system service → all 5 FoodNest pipelines running.

---

## Architecture

```
EC2 Instance
├── Jenkins  (systemd service, port 8080)
├── Docker   (installed on host, jenkins user has access)
└── Trivy    (installed on host, available to all pipeline builds)
```

Jenkins runs directly on the OS — no containers involved for Jenkins itself. Docker is installed on the same EC2 instance and pipelines call it directly.

---

## EC2 Prerequisites

### Recommended instance size

| Resource | Minimum | Recommended |
|---|---|---|
| Instance type | t3.medium (2 vCPU, 4 GB) | t3.large (2 vCPU, 8 GB) |
| Root volume | 20 GB | 30 GB |
| OS | Amazon Linux 2023 **or** Ubuntu 22.04/24.04 LTS | |

Maven builds + Docker image layers consume significant disk. Start with at least 30 GB.

### Security Group rules (inbound)

| Port | Protocol | Source | Purpose |
|---|---|---|---|
| 22 | TCP | Your IP | SSH access |
| 8080 | TCP | 0.0.0.0/0 | Jenkins UI + GitHub webhooks |

---

## Step 1 — Install Java 17

Jenkins requires Java 17 or 21.

### Amazon Linux 2023

```bash
sudo dnf install -y java-17-amazon-corretto-headless
java -version
```

### Ubuntu 22.04 / 24.04

```bash
sudo apt-get update -y
sudo apt-get install -y fontconfig openjdk-17-jre
java -version
```

---

## Step 2 — Install Jenkins

### Amazon Linux 2023

```bash
# Add Jenkins repo
sudo wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key

# Install
sudo dnf install -y jenkins

# Enable and start
sudo systemctl enable jenkins
sudo systemctl start jenkins

# Verify it is running
sudo systemctl status jenkins
```

### Ubuntu 22.04 / 24.04

```bash
# Add Jenkins repo
sudo wget -O /usr/share/keyrings/jenkins-keyring.asc \
    https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
    https://pkg.jenkins.io/debian-stable binary/" \
    | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

# Install
sudo apt-get update -y
sudo apt-get install -y jenkins

# Enable and start
sudo systemctl enable jenkins
sudo systemctl start jenkins

# Verify it is running
sudo systemctl status jenkins
```

Jenkins is now running at `http://<EC2-PUBLIC-IP>:8080`.

---

## Step 3 — Install Docker

The pipelines build and scan Docker images so Docker must be on the same EC2 instance.

### Amazon Linux 2023

```bash
sudo dnf install -y docker
sudo systemctl enable docker
sudo systemctl start docker
```

### Ubuntu 22.04 / 24.04

```bash
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
    | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io
sudo systemctl enable docker
sudo systemctl start docker
```

### Add jenkins user to the docker group

This lets pipeline scripts call `docker build` without `sudo`:

```bash
sudo usermod -aG docker jenkins
```

**Restart Jenkins to apply the group change:**

```bash
sudo systemctl restart jenkins
```

Verify:

```bash
# Switch to jenkins user and test docker access
sudo -u jenkins docker info | grep "Server Version"
```

You should see a server version with no "permission denied" error.

---

## Step 4 — Install Trivy

Pre-installing Trivy on the EC2 host means the "Install Trivy" pipeline stage skips the download and runs instantly.

```bash
# Works on both Amazon Linux 2023 and Ubuntu
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh \
    | sudo sh -s -- -b /usr/local/bin v0.50.1

trivy --version
```

---

## Step 5 — Install Maven (for local test runs — optional)

The pipeline uses Maven inside the Docker build stage, so this is optional. Install it if you want to run `mvn` commands directly on the host too.

### Amazon Linux 2023

```bash
sudo dnf install -y maven
mvn -version
```

### Ubuntu 22.04 / 24.04

```bash
sudo apt-get install -y maven
mvn -version
```

---

## Step 6 — Unlock Jenkins

### Get the initial admin password

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Open `http://<EC2-PUBLIC-IP>:8080` in your browser, paste the password, and click **Continue**.

---

## Step 7 — Install Plugins

On the "Customize Jenkins" screen choose **"Install suggested plugins"** and wait for it to complete.

Then install the additional plugins below.

### Go to: Manage Jenkins → Plugins → Available plugins

Search and install each:

| Plugin | Why needed |
|---|---|
| **Pipeline** | Core declarative pipeline support (usually pre-installed) |
| **Git** | Git/GitHub SCM checkout |
| **GitHub** | GitHub webhook trigger integration |
| **Credentials Binding** | `withCredentials` blocks in pipelines |
| **Workspace Cleanup** | `cleanWs()` in `post` blocks |
| **Timestamper** | `timestamps()` option adds timestamps to logs |
| **Pipeline Stage View** | Visual stage progress bar in Jenkins UI |
| **Blue Ocean** *(optional)* | Modern pipeline UI at `/blue` |
| **AnsiColor** *(optional)* | Colored console output |

Select all → **Install** → check **"Restart Jenkins when installation is complete"**.

Jenkins will restart automatically. Log back in with `admin` and the same initial password.

---

## Step 8 — Configure Credentials

Go to: **Manage Jenkins → Credentials → System → Global credentials (unrestricted) → Add Credentials**

### 8a — GitHub Credentials

| Field | Value |
|---|---|
| Kind | Username with password |
| Username | your GitHub username |
| Password | GitHub Personal Access Token with `repo` scope |
| ID | `github-credentials` |
| Description | GitHub access for FoodNest |

**Create a GitHub PAT:**
GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token → check `repo` → Generate.

### 8b — Docker Hub Credentials

| Field | Value |
|---|---|
| Kind | Username with password |
| Username | your Docker Hub username |
| Password | Docker Hub access token |
| ID | `dockerhub-credentials` |
| Description | Docker Hub push for FoodNest images |

**Create a Docker Hub token:**
hub.docker.com → Account Settings → Security → New Access Token → copy it immediately (shown once).

---

## Step 9 — Create the 5 Pipeline Jobs

Repeat these exact steps for each service. The only thing that changes per job is the **name** and **script path**.

### Job names and Jenkinsfile paths

| Job Name | Script Path |
|---|---|
| `foodnest-auth-service` | `auth-service/Jenkinsfile` |
| `foodnest-catalog-order-service` | `catalog-order-service/Jenkinsfile` |
| `foodnest-inventory-app` | `inventory-notification-service/inventory-app/Jenkinsfile` |
| `foodnest-notification-worker` | `inventory-notification-service/notification-worker/Jenkinsfile` |
| `foodnest-frontend` | `frontend/Jenkinsfile` |

### Steps for each job

1. **Dashboard → New Item**
2. Enter the job name from the table above
3. Select **Pipeline** → click **OK**

**General section**
- Check **Discard old builds** → Max builds to keep: `10`
- Check **GitHub project** → enter your repo URL:
  `https://github.com/<YOUR_ORG>/FoodNest`

**Build Triggers section**
- Check **GitHub hook trigger for GITScm polling**

**Pipeline section**
- Definition: **Pipeline script from SCM**
- SCM: **Git**
- Repository URL: `https://github.com/<YOUR_ORG>/FoodNest.git`
- Credentials: select `github-credentials`
- Branches to build: `*/main`
- Script Path: *(use the path from the table above)*
- Check **Lightweight checkout**

4. Click **Save**

Repeat for all 5 jobs.

---

## Step 10 — Set Jenkins URL

**Manage Jenkins → System → Jenkins Location → Jenkins URL**

Set it to:

```
http://<EC2-PUBLIC-IP>:8080/
```

This is required for GitHub webhook callbacks to reach Jenkins. Click **Save**.

---

## Step 11 — Configure GitHub Webhook

This makes GitHub push events automatically trigger the matching pipeline.

### On GitHub

1. Go to your FoodNest repo → **Settings → Webhooks → Add webhook**

| Field | Value |
|---|---|
| Payload URL | `http://<EC2-PUBLIC-IP>:8080/github-webhook/` |
| Content type | `application/json` |
| Which events | Just the **push** event |

2. Click **Add webhook**

GitHub sends a test ping. Look for the green tick under **Recent Deliveries** — that confirms Jenkins is reachable.

> If you see a red X, check your EC2 security group has port 8080 open to `0.0.0.0/0`.

---

## Step 12 — Run Your First Build

1. Open `foodnest-auth-service` → click **Build Now**
2. Click the build number → **Console Output**

### What you should see

```
[Pipeline] Start of Pipeline
[Pipeline] stage (Checkout)
  Cloning https://github.com/<ORG>/FoodNest.git ...
[Pipeline] stage (Install Trivy)
  Trivy already installed: Version: 0.50.1
[Pipeline] stage (Trivy Filesystem Scan)
  trivy fs --severity HIGH,CRITICAL auth-service/ ...
  === Filesystem Scan Report ===
[Pipeline] stage (Build Docker Image)
  docker build -t foodwise/auth-service:1 auth-service/ ...
  Successfully built <id>
[Pipeline] stage (Trivy Image Scan)
  trivy image --severity CRITICAL foodwise/auth-service:1 ...
  === Image Scan Report ===
[Pipeline] End of Pipeline
Finished: SUCCESS
```

### View scan reports

**Build → Artifacts** → `trivy-fs-report.txt` and `trivy-image-report.txt`

---

## Step 13 — Run All 5 Pipelines

Trigger in this order to match the service dependency chain:

1. `foodnest-auth-service`
2. `foodnest-catalog-order-service`
3. `foodnest-inventory-app`
4. `foodnest-notification-worker`
5. `foodnest-frontend`

**Or trigger all at once via curl** (get your API token first: top-right username → Configure → API Token → Add new Token):

```bash
EC2_IP="<YOUR-EC2-IP>"
API_TOKEN="<YOUR-API-TOKEN>"

for JOB in foodnest-auth-service foodnest-catalog-order-service \
            foodnest-inventory-app foodnest-notification-worker \
            foodnest-frontend; do
  curl -s -X POST "http://admin:${API_TOKEN}@${EC2_IP}:8080/job/${JOB}/build"
  echo "Triggered: ${JOB}"
done
```

---

## Troubleshooting

### Jenkins won't start

```bash
# Check the journal for error details
sudo journalctl -u jenkins -n 50 --no-pager

# Most common cause: wrong Java version
java -version   # must be 17 or 21
```

### "docker: command not found" in pipeline

The `jenkins` user can't find Docker:

```bash
# Confirm docker is on PATH for jenkins
sudo -u jenkins which docker

# Confirm jenkins is in docker group
groups jenkins
```

If the group is missing, re-run:

```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

### "permission denied" on /var/run/docker.sock

The docker group change hasn't taken effect yet:

```bash
sudo systemctl restart jenkins
# then re-run the build
```

### "trivy: command not found"

Trivy was not installed in Step 4, or `/usr/local/bin` is not on the jenkins user's PATH:

```bash
sudo -u jenkins which trivy
sudo -u jenkins trivy --version
```

If missing, re-run the install from Step 4.

### Trivy Image Scan fails (CRITICAL CVEs found)

The built image has unpatched critical CVEs. Options:

1. **Upgrade the base image** — edit the `FROM` line in the Dockerfile to a newer patch release
2. **Temporarily unblock** — change `--exit-code 1` to `--exit-code 0` in the Jenkinsfile while you investigate
3. **Suppress known false-positives** — add a `.trivyignore` file in the service directory:
   ```
   # .trivyignore
   CVE-2023-XXXXX
   ```

### GitHub webhook delivers but build doesn't trigger

```bash
# Check Jenkins logs for the incoming webhook
sudo tail -f /var/log/jenkins/jenkins.log | grep -i "webhook\|github\|trigger"
```

Verify:
- **Jenkins URL** is set in Manage Jenkins → System (Step 10)
- The job has **"GitHub hook trigger for GITScm polling"** checked
- Port 8080 is open in the EC2 security group

### Disk space running low

```bash
df -h /

# Remove dangling Docker images and stopped containers
docker system prune -f

# Remove old Jenkins workspaces
# Manage Jenkins → job → Workspaces → Wipe out current workspace
```

---

## Adding Docker Hub Push (Production)

Add this stage after `Trivy Image Scan` in each Jenkinsfile to push images on `main` branch builds:

```groovy
stage('Push Docker Image') {
    when { branch 'main' }
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
                docker logout
            '''
        }
    }
}
```

---

## Quick Reference

| Task | Command / Location |
|---|---|
| Jenkins status | `sudo systemctl status jenkins` |
| Start / Stop / Restart | `sudo systemctl start\|stop\|restart jenkins` |
| Jenkins logs | `sudo tail -f /var/log/jenkins/jenkins.log` |
| Initial admin password | `sudo cat /var/lib/jenkins/secrets/initialAdminPassword` |
| Jenkins home directory | `/var/lib/jenkins/` |
| Jenkins UI | `http://<EC2-IP>:8080` |
| Add credentials | Manage Jenkins → Credentials → Global → Add Credentials |
| Install plugins | Manage Jenkins → Plugins → Available plugins |
| Pipeline jobs | Dashboard → `foodnest-*` |
| Build scan reports | Build → Artifacts → `trivy-*.txt` |
