# Phase 11.1 — Jenkins Continuous Integration Pipeline for Cloud IDE

## 1. Overview & Objective

Phase 11.1 establishes an automated Continuous Integration (CI) pipeline for Cloud IDE using **Jenkins Declarative Pipeline** syntax. The pipeline automatically checks out the repository, installs frontend dependencies, compiles and bundles the React frontend, runs unit and security tests for both the Spring Boot backend and host execution worker, validates the Docker Compose configuration, builds tagged production Docker container images, and collects JUnit test reports.

---

## 2. Environment & Prerequisites

### 2.1 Local Host Configuration
- **Operating System**: Windows 11 (amd64)
- **Jenkins Controller**: Native Windows service (Version 2.568.3)
  - **URL**: `http://localhost:8081`
  - **Service Path**: `C:\Program Files\Jenkins\jenkins.exe`
  - **Configuration**: `C:\Program Files\Jenkins\jenkins.xml` (`--httpPort=8081`)
  - **Jenkins Home**: `C:\ProgramData\Jenkins\.jenkins`
- **Installed Toolchain**:
  - **Java**: Java 25.0.1 (Oracle JDK) / Java 21 runtime
  - **Maven**: Apache Maven 3.9.16 (`C:\Users\Welcome\Documents\malven\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd`)
  - **Node.js**: v24.13.0
  - **npm**: 11.6.2
  - **Docker Engine / CLI**: Docker version 29.7.2 (Docker Desktop)
  - **Git**: git version 2.45.1.windows.1

### 2.2 Required Jenkins Plugins
The following plugins are already installed and verified in `C:\ProgramData\Jenkins\.jenkins\plugins`:
- **Pipeline (`workflow-aggregator`)**: Enables Declarative and Scripted Pipeline syntax.
- **Git (`git`, `git-client`)**: Provides SCM checkout capabilities.
- **JUnit (`junit`)**: Collects, archives, and displays Surefire test result XMLs.
- **Credentials Binding (`credentials-binding`, `credentials`)**: Handles secure credentials injection without logging.
- **Timestamper (`timestamper`)**: Adds timestamps to console log output.

---

## 3. Platform Independence & Windows Execution

### 3.1 Resolving `'sh' is not recognized`
In standard Linux environments, Jenkins steps invoke `/bin/sh`. On a Windows Jenkins agent (such as the default controller agent running on Windows), executing `sh '...'` fails with:
```text
'sh' is not recognized as an internal or external command, operable program or batch file.
```
Or:
```text
Cannot run program "sh": CreateProcess error=2, The system cannot find the file specified
```

### 3.2 Dual-OS Step Abstraction (`isUnix()`)
To ensure compatibility across both native Windows agents and containerized Linux agents, all build and test steps in `Jenkinsfile` use a conditional abstraction:
```groovy
script {
    if (isUnix()) {
        sh 'npm ci'
        sh 'npm run build'
    } else {
        bat 'call npm ci'
        bat 'call npm run build'
    }
}
```

### 3.3 The `call` Keyword in Batch Steps
In Windows `cmd.exe`, batch scripts like `npm.cmd` and `mvn.cmd` terminate the batch runner upon completion unless invoked with `call`. Using `call npm ci` and `call mvn clean test` guarantees that the Windows batch process continues to subsequent commands.

---

## 4. Pipeline Stages & Workflow

```
[ SCM Checkout ]
       ↓
[ Stage 1: Frontend Build ]
  → npm ci
  → npm run build
       ↓
[ Stage 2: Backend Tests ]
  → dir('backend') { mvn clean test } (19 tests)
       ↓
[ Stage 3: Worker Tests ]
  → dir('execution-worker') { mvn clean test } (6 tests)
       ↓
[ Stage 4: Docker Compose Validation ]
  → docker compose config
       ↓
[ Stage 5: Docker Image Build ]
  → docker build -t cloud-ide-frontend:${IMAGE_TAG} .
  → docker build -t cloud-ide-backend:${IMAGE_TAG} ./backend
       ↓
[ Post Actions: Test Reporting ]
  → junit '**/target/surefire-reports/*.xml'
```

### Stage Summary

| Stage Name | Command (Windows / Linux) | Directory | Purpose |
| :--- | :--- | :--- | :--- |
| **Checkout** | `checkout scm` | Workspace root | Pulls latest code from GitHub `main` branch |
| **Frontend Build** | `call npm ci` / `npm ci`<br>`call npm run build` / `npm run build` | Workspace root | Restores dependencies via `package-lock.json`, compiles TypeScript, and builds production Vite bundle in `dist/` |
| **Backend Tests** | `call mvn clean test` / `mvn clean test` | `./backend` | Executes all 19 Spring Boot unit, security, and execution queue tests |
| **Worker Tests** | `call mvn clean test` / `mvn clean test` | `./execution-worker` | Executes all 6 unit and sandbox validation tests |
| **Docker Compose Validation** | `call docker compose config` / `docker compose config` | Workspace root | Validates Docker Compose service definitions, volumes, and environment variables |
| **Docker Image Build** | `call docker build -t cloud-ide-frontend:${IMAGE_TAG} .`<br>`call docker build -t cloud-ide-backend:${IMAGE_TAG} ./backend` | Root & `./backend` | Builds hardened production container images tagged with `build-${BUILD_NUMBER}` |
| **Test Report Archival** | `junit testResults: '**/target/surefire-reports/*.xml'` | Post action (always) | Aggregates test failure/success statistics into Jenkins UI |

---

## 5. Jenkins Job Configuration Guide

### Step 1: Open Jenkins Dashboard
Navigate to `http://localhost:8081` in your browser and log in with your administrative credentials (`harish`).

### Step 2: Create a New Pipeline Job
1. Click **New Item** on the left menu.
2. Enter the name: `cloud-ide-ci`.
3. Select **Pipeline** as the item type.
4. Click **OK**.

### Step 3: Configure SCM & Repository
1. In the job configuration page, scroll down to the **Pipeline** section.
2. Under **Definition**, select **Pipeline script from SCM**.
3. Under **SCM**, select **Git**.
4. Set **Repository URL** to:
   ```text
   https://github.com/HARISH-bit31/Cloud-IDE.git
   ```
5. **Credentials**: If the repository is public, leave as `none`. If private, select or add your GitHub Personal Access Token credential.
6. Under **Branches to build**, set **Branch Specifier** to:
   ```text
   */main
   ```
7. Set **Script Path** to:
   ```text
   Jenkinsfile
   ```
8. Click **Save**.

### Step 4: Run the Build
1. In the `cloud-ide-ci` project page, click **Build Now**.
2. Monitor build progress in the **Stage View** and click on the build number to inspect console logs.
3. Once completed, inspect the **Test Result** trend tab to view unit test metrics from both Maven projects.

---

## 6. Security & Credentials Policy

1. **No Credentials in Jenkinsfile**: No API keys, passwords, database credentials, or worker secrets are stored in the pipeline definition.
2. **Docker Isolation**: The pipeline invokes the host Docker CLI for validation and image building. Docker sockets are never exposed or mounted into application containers.
3. **Sandbox Hardening Preserved**: Container sandbox executions continue to utilize non-root users, memory/CPU bounds, and network isolation (`--network none`).
4. **Git & Secret Hygiene**: Secret manifests (`k8s/secret.yaml`), `.env` files, and local test scripts are excluded via `.dockerignore` and `.gitignore`.

---

## 7. Local Validation Results

All pipeline steps were verified directly on the local environment before committing the pipeline configuration:

| Check | Target / Command | Result |
| :--- | :--- | :--- |
| **Frontend Build** | `npm run build` | **PASS**: 1918 modules transformed, built in 4.18s |
| **Backend Unit Tests** | `mvn clean test` (in `./backend`) | **PASS**: 19/19 passed, 0 failures, 0 errors |
| **Worker Unit Tests** | `mvn clean test` (in `./execution-worker`) | **PASS**: 6/6 passed, 0 failures, 0 errors |
| **Docker Compose Config** | `docker compose config` | **PASS**: Valid schema, all services reconciled |
| **Frontend Image Build** | `docker build -t cloud-ide-frontend:ci-test .` | **PASS**: Production Alpine image built cleanly |
| **Backend Image Build** | `docker build -t cloud-ide-backend:ci-test ./backend` | **PASS**: Unprivileged Alpine JRE image built cleanly |
| **Kubernetes Stability** | `kubectl get pods -n cloud-ide` | **PASS**: All 9 pods remain 1/1 Running & healthy |
