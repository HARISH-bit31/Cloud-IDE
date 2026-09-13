# Cloud IDE — Kubernetes Deployment (Phase 9.3)

This directory contains the production-style Kubernetes manifests for deploying the Cloud IDE application (Frontend, Backend, MySQL, Network Policies, Ingress, Pod Disruption Budgets) to a local Kubernetes cluster such as **Docker Desktop Kubernetes**.

For an in-depth analysis of the security boundary, host communication, and future cloud execution architectures, refer to [`execution-worker-architecture.md`](file:///c:/Users/Welcome/Documents/projects/Cloud-IDE/k8s/execution-worker-architecture.md).

---

## 1. Architecture Overview

```
                               Kubernetes Cluster
                             (Namespace: cloud-ide)
                                        │
           ┌────────────────────────────┼────────────────────────────┐
           │                            │                            │
   Frontend Pods (x2)            Backend Pods (x2)              MySQL Pod (x1)
     (Nginx/React)            (Spring Boot Java 21)              (MySQL 8.0)
     ClusterIP: 80                ClusterIP: 8088              ClusterIP: 3306
    (NodePort: 30080)                   │                            │
           │                            │                            ▼
           │ /api proxy                 │ JDBC                   mysql-pvc
           └───────────────────────────►│                          (5Gi)
                                        │ HTTP (Worker Secret)
                                        ▼
                            host.docker.internal:8090
                      ┌───────────────────────────────────┐
                      │    Execution Worker Service       │
                      │  (Host Windows Machine - Port 8090│
                      └─────────────────┬─────────────────┘
                                        │ ProcessBuilder API
                                        ▼
                               Docker Desktop Engine
                                        │
            ┌───────────────────────────┼───────────────────────────┐
            ▼                           ▼                           ▼
     Java Sandbox                Python Sandbox               C / C++ Sandbox
  (cloud-ide-java:latest)     (cloud-ide-python:latest)    (cloud-ide-c/cpp:latest)
```

---

## 2. Ingress & Access Methods

### Ingress (`cloud-ide.local`)
- **Ingress Resource**: [`k8s/ingress.yaml`](file:///c:/Users/Welcome/Documents/projects/Cloud-IDE/k8s/ingress.yaml) routes traffic based on hostname `cloud-ide.local`:
  - `/api` $\rightarrow$ `backend:8088`
  - `/` $\rightarrow$ `frontend:80`
- **Hosts File Configuration**:
  To access via Ingress when an Ingress controller (e.g., Ingress-Nginx) is enabled, map `127.0.0.1` in your hosts file (`C:\Windows\System32\drivers\etc\hosts` or `/etc/hosts`):
  ```text
  127.0.0.1 cloud-ide.local
  ```
  Then browse to [http://cloud-ide.local](http://cloud-ide.local).

### NodePort Fallback
- **Frontend NodePort**: Port **`30080`** (`http://localhost:30080`) is maintained as a reliable development access method without requiring external DNS or Ingress controllers.

---

## 3. High Availability, Scaling & Rolling Updates

### Replica Configuration
- **Frontend**: `replicas: 2` (stateless Nginx + React bundle).
- **Backend**: `replicas: 2` (stateless Spring Boot REST API).
- **MySQL**: `replicas: 1` (single-instance persistent database bound to `mysql-pvc`).

### Zero-Downtime Rolling Updates
Deployments utilize `strategy.type: RollingUpdate` with:
- `maxUnavailable: 0` (guarantees at least 2 active pods at all times during rollout).
- `maxSurge: 1` (spins up new version pod before tearing down old version).

### Pod Disruption Budget (PDB)
[`k8s/pdb.yaml`](file:///c:/Users/Welcome/Documents/projects/Cloud-IDE/k8s/pdb.yaml) defines `minAvailable: 1` for both `backend` and `frontend` deployments, preventing voluntary disruptions (node drains, cluster upgrades) from causing downtime.

### Horizontal Pod Autoscaling (HPA) & Metrics Status
- `metrics-server` is not installed by default in Docker Desktop Kubernetes (`Error from server (NotFound): deployments.apps "metrics-server" not found`).
- HPA is therefore **deferred** until a production cluster with metrics-server / Prometheus adapter is configured.
- Manual scaling is fully supported and verified via `kubectl scale deployment <name> --replicas=N -n cloud-ide`.

---

## 4. Health Checks & Graceful Shutdown

1. **Startup Probes**:
   - `backend`: `httpGet /api/health` on port 8088 (`failureThreshold: 12`, `periodSeconds: 5`). Allows slow Spring Boot bootups without trigger premature liveness kills.
   - `frontend`: `httpGet /` on port 80.
   - `mysql`: `mysqladmin ping` via exec probe (`failureThreshold: 12`, `periodSeconds: 5`).
2. **Readiness Probes**:
   - Traffic is only routed to pods once readiness probes succeed.
3. **Liveness Probes**:
   - Continuously verify service health and automatically restart deadlocked containers.
4. **Graceful Shutdown**:
   - Backend configured with `server.shutdown=graceful` and `spring.lifecycle.timeout-per-shutdown-phase=20s`.
   - Pod spec sets `terminationGracePeriodSeconds: 30`, allowing in-flight HTTP requests and database queries to finish cleanly before termination.

---

## 5. Security Hardening & RBAC

- **RBAC**: Application pods do not interact with the Kubernetes API. `automountServiceAccountToken: false` is configured on all pods to eliminate credential token exposure.
- **Rootless Execution**: Backend and sandbox containers run as unprivileged users (`runAsNonRoot: true`, `allowPrivilegeEscalation: false`).
- **Network Isolation**: Ephemeral code execution sandboxes run with `--network none` on the host, preventing network access or lateral movement.
- **Docker Socket Isolation**: `/var/run/docker.sock` is **never** mounted in Kubernetes pods.
- **Secrets Management**: Sensitive credentials (`MYSQL_PASSWORD`, `JWT_SECRET`, `EXECUTION_WORKER_SECRET`) are stored in Kubernetes Secrets (`cloud-ide-secrets`).

---

## 6. Deployment & Operation Commands

### Deploy All Resources
```powershell
# 1. Apply namespace, configs, PVCs, deployments, services, ingress, and PDB
kubectl apply -k k8s/

# 2. Verify all pods and services are Running and Ready (2/2 frontend, 2/2 backend, 1/1 mysql)
kubectl get pods,svc,ingress,pdb -n cloud-ide
```

### Manual Scaling
```powershell
# Scale backend to 3 replicas
kubectl scale deployment backend --replicas=3 -n cloud-ide

# Scale backend back to 2 replicas
kubectl scale deployment backend --replicas=2 -n cloud-ide
```

### Rolling Updates & Rollback
```powershell
# Trigger rollout restart
kubectl rollout restart deployment/backend -n cloud-ide

# Check rollout progress
kubectl rollout status deployment/backend -n cloud-ide

# View rollout history
kubectl rollout history deployment/backend -n cloud-ide

# Rollback to previous revision
kubectl rollout undo deployment/backend -n cloud-ide
```

### Inspect Logs
```powershell
# Backend logs (all replicas)
kubectl logs -l app=backend -n cloud-ide -f

# Frontend logs
kubectl logs -l app=frontend -n cloud-ide -f

# MySQL logs
kubectl logs -l app=mysql -n cloud-ide -f
```

### Teardown (Preserving Database Volume)
```powershell
# Delete all resources except PVC
kubectl delete -k k8s/

# To explicitly delete persistent storage:
kubectl delete pvc mysql-pvc -n cloud-ide
```

---

## 8. Observability Stack (Phase 9.4)

Cloud IDE includes a complete local Kubernetes observability stack consisting of **Prometheus** (metrics scraping and storage), **Grafana** (dashboards and visualization), **Loki** (log aggregation), and **Promtail** (pod log shipping).

### Observability Topology

```
                         Cloud IDE Pods
              ┌─────────────────┴─────────────────┐
              ▼                                   ▼
      Spring Boot Actuator                   Pod Log Files
     (/actuator/prometheus)             (/var/log/pods/*/*/*.log)
              │                                   │
              ▼                                   ▼
      Prometheus (:9090)                 Promtail (DaemonSet)
              │                                   │
              │                                   ▼
              │                             Loki (:3100)
              │                                   │
              └─────────────────┬─────────────────┘
                                │
                                ▼
                         Grafana (:3000)
                   (Pre-provisioned Dashboard)
```

### Accessing Observability Tools (Port-Forwarding)

Observability components are internal to the `cloud-ide` namespace and not exposed publicly. Access them securely using `kubectl port-forward`:

```powershell
# 1. Grafana Dashboard (Credentials configured via Secret: grafana-admin)
kubectl port-forward svc/grafana 3000:3000 -n cloud-ide
# URL: http://localhost:3000 (Dashboard: "Cloud IDE Platform Overview")

# 2. Prometheus UI & API
kubectl port-forward svc/prometheus 9090:9090 -n cloud-ide
# URL: http://localhost:9090

# 3. Loki API & Log Streams
kubectl port-forward svc/loki 3100:3100 -n cloud-ide
# URL: http://localhost:3100
```

### Key Custom Micrometer Metrics

| Metric Name | Type | Description | Labels / Dimensions |
| :--- | :--- | :--- | :--- |
| `cloud_ide_executions_total` | Counter | Total execution requests started/completed | `language`, `status` |
| `cloud_ide_execution_duration_seconds` | Summary/Histogram | Execution latency in seconds | `language`, `status` |
| `cloud_ide_execution_failures_total` | Counter | Execution failures and errors | `language`, `reason` |
| `cloud_ide_active_executions` | Gauge | Currently running execution jobs | `application` |
| `jvm_memory_used_bytes` | Gauge | JVM memory consumption | `area`, `id` |
| `hikaricp_connections_active` | Gauge | Active MySQL connection pool connections | `pool` |

### Structured Logging & Correlation IDs
- All HTTP requests entering the Spring Boot backend receive a unique `X-Request-ID` (correlation ID) injected into SLF4J `MDC` and returned in the HTTP response headers.
- Promtail continuously tails pod logs from `/var/log/pods` and pushes them to Loki with labels `job="kubernetes-pods"` and container-level metadata.
- Grafana integrates both Prometheus metrics and Loki log streams in a single unified dashboard.

---

## 9. Comparative Architecture Matrix

| Aspect | Docker Compose | Local Kubernetes (Phase 9.4) | Future Production (Phase 9.5+) |
| :--- | :--- | :--- | :--- |
| **Frontend Access** | `http://localhost:5173` | `http://cloud-ide.local` / `http://localhost:30080` | `https://cloud-ide.io` (TLS Ingress / CDN) |
| **Backend Replicas** | 1 container | 2 pods (RollingUpdate, PDB) | Auto-scaling (HPA / KEDA) |
| **Frontend Replicas** | 1 container | 2 pods (RollingUpdate, PDB) | Auto-scaling (HPA) |
| **MySQL** | Docker container | 1 pod + 5Gi PVC | Managed Cloud DB (AWS RDS / Cloud SQL) |
| **Code Execution** | Host Worker (`8090`) | Host Worker (`8090`) | Asynchronous Queue + MicroVMs (gVisor/Firecracker) |
| **Metrics & Monitoring** | None / local actuator | Prometheus + Custom Micrometer Metrics | Managed Prometheus / Grafana Cloud |
| **Log Aggregation** | Docker logs | Promtail + Loki | Managed Loki / OpenSearch / CloudWatch |
