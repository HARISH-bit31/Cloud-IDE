# Phase 11.2 — Jenkins Automated Docker Deployment to Local Kubernetes

## 1. Overview & Objective

Phase 11.2 extends the existing Jenkins Continuous Integration (CI) pipeline into a Continuous Deployment (CD) pipeline capable of safely deploying newly compiled frontend and backend Docker images to the local **Docker Desktop Kubernetes** cluster in the `cloud-ide` namespace.

The deployment preserves all application architecture, execution worker sandbox restrictions, MySQL persistent volumes, and Prometheus/Grafana/Loki observability services without downtime or risk of data loss.

---

## 2. Deployment Architecture

```
[ Developer Commit / Manual Trigger ]
                ↓
    [ Stage 1: Checkout SCM ]
                ↓
    [ Stage 2: Frontend Build (npm ci && npm run build) ]
                ↓
    [ Stage 3: Backend Tests (mvn clean test - 19 tests) ]
                ↓
    [ Stage 4: Worker Tests (mvn clean test - 6 tests) ]
                ↓
    [ Stage 5: Docker Compose Validation (docker compose config) ]
                ↓
    [ Stage 6: Docker Image Build (cloud-ide-*:build-${BUILD_NUMBER}) ]
                ↓
   ┌──────────────────────────────────────────────────┐
   │ Gate: Is DEPLOY_TO_LOCAL_K8S == true?           │
   └──────────────────────────────────────────────────┘
            │                                 │
           YES                                NO
            ↓                                 ↓
[ Stage 7: K8s Preflight Check ]      [ Skip Deployment ]
            ↓                                 ↓
[ Stage 8: Deploy Images ]           [ Publish Test Reports ]
            ↓
[ Stage 9: Wait for Rollout ]
            ↓
[ Stage 10: Health & Smoke Tests ]
            ↓
[ Post Actions: Test Archival & Status ]
            │ (On Failure)
            ↓
    [ Automatic Rollback: kubectl rollout undo ]
```

---

## 3. Pipeline Configuration & Manual Approval Gate

### 3.1 The `DEPLOY_TO_LOCAL_K8S` Parameter
To prevent unintended deployments during routine test runs or experimental branches, the pipeline introduces a boolean parameter in [`Jenkinsfile`](file:///c:/Users/Welcome/Documents/projects/Cloud-IDE/Jenkinsfile):
```groovy
parameters {
    booleanParam(
        name: 'DEPLOY_TO_LOCAL_K8S',
        defaultValue: false,
        description: 'Deploy built images to the local Docker Desktop Kubernetes cluster (cloud-ide namespace) after all tests pass.'
    )
}
```

- **When `DEPLOY_TO_LOCAL_K8S` is `false` (Default)**:
  The pipeline executes Stages 1–6 (build and test), records test results, and exits with `SUCCESS` without touching the Kubernetes cluster.
- **When `DEPLOY_TO_LOCAL_K8S` is `true`**:
  The pipeline executes Stages 1–6, and upon passing all tests, proceeds through Stages 7–10 to update the deployments, await healthy rollout, and execute regression smoke tests.

---

## 4. Environment & Permissions Setup

### 4.1 Jenkins Agent & Service Account Capabilities
- **Jenkins Service**: Running as `LocalSystem` on Windows (`http://localhost:8081`).
- **Docker Access**: Docker CLI communicates with the host Docker Engine via the Windows named pipe `\\.\pipe\docker_engine`, accessible by the administrator and system accounts.
- **Kubeconfig Resolution**:
  Standard user kubeconfig resides at `C:\Users\Welcome\.kube\config`. Because `LocalSystem` defaults to `C:\Windows\System32\config\systemprofile`, the pipeline explicitly exports `KUBECONFIG`:
  ```groovy
  environment {
      IMAGE_TAG = "build-${env.BUILD_NUMBER ?: 'manual'}"
      KUBECONFIG = env.KUBECONFIG ?: (isUnix() ? "${env.HOME}/.kube/config" : 'C:\\Users\\Welcome\\.kube\\config')
      DOCKER_CONFIG = env.DOCKER_CONFIG ?: (isUnix() ? "${env.HOME}/.docker" : 'C:\\Users\\Welcome\\.docker')
  }
  ```

---

## 5. Pipeline Stages Walkthrough

### Stage 7: Kubernetes Preflight
Validates cluster context, namespace presence, existing deployments, and local image availability:
```bat
call kubectl version --client
call kubectl config current-context
call kubectl get namespace cloud-ide
call kubectl get deployment backend frontend -n cloud-ide
call docker image inspect cloud-ide-frontend:%IMAGE_TAG% > nul
call docker image inspect cloud-ide-backend:%IMAGE_TAG% > nul
```

### Stage 8: Deploy Frontend and Backend
Applies atomic rolling image updates exclusively to the frontend and backend deployments:
```bat
call kubectl set image deployment/frontend frontend=cloud-ide-frontend:%IMAGE_TAG% -n cloud-ide
call kubectl set image deployment/backend backend=cloud-ide-backend:%IMAGE_TAG% -n cloud-ide
```

### Stage 9: Wait for Rollout Completion
Monitors rollout status with strict timeouts to verify that new pods pass startup, readiness, and liveness probes before old pods are decommissioned:
```bat
call kubectl rollout status deployment/backend -n cloud-ide --timeout=180s
call kubectl rollout status deployment/frontend -n cloud-ide --timeout=180s
call kubectl get pods -n cloud-ide -l "app in (frontend, backend)"
```

### Stage 10: Health & Regression Verification
Executes [`scripts/smoke_test.cjs`](file:///c:/Users/Welcome/Documents/projects/Cloud-IDE/scripts/smoke_test.cjs) which verifies 8 core aspects of cluster health:
1. **Frontend Web Server**: HTTP 200 HTML response.
2. **Backend Actuator / Health**: HTTP 200 `status: "UP"`.
3. **Authentication**: User registration and JWT login.
4. **Project CRUD**: Creation and retrieval of project records.
5. **Execution Worker Connectivity**: Code submission and polling until `status: "SUCCESS"`.
6. **Prometheus Observability**: Active scrape target `backend:8088` is `health: "up"`.
7. **Loki Logging Service**: Ingestion endpoint `/ready` returns `ready`.
8. **Grafana Dashboard Service**: Health endpoint `/api/health` returns `database: "ok"`.

---

## 6. Automated Rollback Strategy

If any deployment stage or smoke test fails, the pipeline immediately triggers an automatic rollback in the `post { failure { ... } }` block:
```groovy
post {
    failure {
        script {
            if (params.DEPLOY_TO_LOCAL_K8S) {
                echo "[Rollback] Deployment or verification failed. Reverting to previous revision..."
                bat 'call kubectl rollout undo deployment/backend -n cloud-ide'
                bat 'call kubectl rollout undo deployment/frontend -n cloud-ide'
                bat 'call kubectl rollout status deployment/backend -n cloud-ide --timeout=120s'
                bat 'call kubectl rollout status deployment/frontend -n cloud-ide --timeout=120s'
                bat 'call kubectl get events -n cloud-ide --sort-by=.metadata.creationTimestamp'
            }
        }
    }
}
```

### Rollback Guarantees:
- **No Data Loss**: The MySQL database, PersistentVolumeClaims (`mysql-pvc`), and cluster secrets remain untouched.
- **Fast Recovery**: Restores the previous known-healthy container image reference.
- **Failure Transparency**: Kubernetes cluster events are output to the Jenkins console log without printing sensitive environment variables or secrets.

---

## 7. Security & Hardening Controls

1. **No Cluster Credentials in Git**: All cluster interactions leverage the host `KUBECONFIG` file with least-privilege service account scoping.
2. **Local Image Access**: Docker Desktop Kubernetes directly resolves locally tagged Docker images (`imagePullPolicy: IfNotPresent`), eliminating external registry dependencies or credentials.
3. **Sandbox Isolation Maintained**: Code executions in the backend continue to dispatch to the host execution worker (`http://host.docker.internal:8090`) with `--network none` container isolation and memory/CPU limits.
4. **Non-Root Containers**: Frontend runs as an unprivileged Alpine Nginx user; backend runs as non-root `appuser:appgroup` (UID 1000).

---

## 8. Verification Results

| Verification Check | Target / Command | Result |
| :--- | :--- | :--- |
| **Jenkinsfile Syntax** | Declarative linter & structure | **PASS**: Valid declarative pipeline |
| **Local Deployment** | `kubectl set image` | **PASS**: Image tags updated to `phase11.2-test` |
| **Rollout Monitoring** | `kubectl rollout status` | **PASS**: Both deployments rolled out successfully |
| **Post-Deploy Smoke Test** | `node scripts/smoke_test.cjs` | **PASS**: 8/8 tests passed |
| **Rollback Verification** | `kubectl rollout undo` | **PASS**: Successfully reverted to previous revision |
| **Post-Rollback Smoke Test** | `node scripts/smoke_test.cjs` | **PASS**: 8/8 tests passed on reverted cluster |
| **Observability Status** | Prometheus, Grafana, Loki | **PASS**: All observability components operational |
