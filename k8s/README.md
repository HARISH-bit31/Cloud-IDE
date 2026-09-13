# Cloud IDE — Kubernetes Deployment (Phase 9.2)

This directory contains the Kubernetes manifests for deploying the Cloud IDE application (Frontend, Backend, MySQL, Network Policies) to a local Kubernetes cluster such as **Docker Desktop Kubernetes**.

For an in-depth analysis of the security boundary, host communication, and future cloud execution architectures, refer to [`execution-worker-architecture.md`](file:///c:/Users/Welcome/Documents/projects/Cloud-IDE/k8s/execution-worker-architecture.md).

---

## 1. Architecture Overview

```
                         Kubernetes Cluster
                      (Namespace: cloud-ide)
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
  Frontend Pod(s)          Backend Pod(s)          MySQL Pod(s)
  (Nginx + React)       (Spring Boot Java 21)       (MySQL 8.0)
          │                      │                      │
    NodePort 30080         ClusterIP 8088         ClusterIP 3306
  (http://localhost:30080)   (http://backend:8088)  (mysql:3306)
          │                      │                      │
          │ /api proxy           │                      ▼
          └─────────────────────►│                 mysql-pvc
                                 │                   (5Gi)
                                 │ HTTP
                                 ▼
                     host.docker.internal:8090
                     Execution Worker (Host)
```

### Components

1. **Namespace (`cloud-ide`)**: Logical isolation boundary for all Cloud IDE Kubernetes resources.
2. **Frontend Deployment & Service (`frontend`)**:
   - Nginx serving the compiled React/Vite/TypeScript single-page application.
   - Nginx reverse-proxies `/api/` requests to `http://backend:8088/api/`.
   - Exposed externally on the host at **`http://localhost:30080`** via a Kubernetes `NodePort` Service (port 30080).
3. **Backend Deployment & Service (`backend`)**:
   - Spring Boot 3.4.3 (Java 21) REST API.
   - Communicates with MySQL internally via `mysql:3306`.
   - Communicates with the Execution Worker via `http://host.docker.internal:8090` using mutual worker secret authentication (`X-Execution-Worker-Secret`).
   - ClusterIP Service exposed internally on port `8088`.
4. **MySQL Deployment, Service, & PVC (`mysql`, `mysql-pvc`)**:
   - MySQL 8.0 container attached to a 5Gi `PersistentVolumeClaim` mounted at `/var/lib/mysql`.
   - ClusterIP Service exposed internally on port `3306`.
5. **ConfigMap & Secret (`cloud-ide-config`, `cloud-ide-secrets`)**:
   - Non-sensitive parameters stored in `cloud-ide-config`.
   - Sensitive credentials (DB passwords, JWT signing key, worker secret) stored securely in `cloud-ide-secrets`.

---

## 2. Prerequisites

1. **Docker Desktop** with **Kubernetes enabled**:
   - Open Docker Desktop $\rightarrow$ Settings $\rightarrow$ Kubernetes $\rightarrow$ Check **Enable Kubernetes** $\rightarrow$ Apply & restart.
2. **`kubectl`** CLI tool installed and configured to point to `docker-desktop` context:
   ```powershell
   kubectl config current-context
   # Expected output: docker-desktop
   ```

---

## 3. Step-by-Step Deployment Guide

### Step 1: Build Local Docker Images

Ensure local Docker images exist in Docker Desktop before deploying to Kubernetes:

```powershell
# Build Frontend Image
docker build -t cloud-ide-frontend:latest .

# Build Backend Image
docker build -t cloud-ide-backend:latest ./backend
```

### Step 2: Create Namespace & Secret

Create the `cloud-ide` namespace and the secret containing database credentials, JWT secret, and execution worker secret:

#### Windows PowerShell:
```powershell
# 1. Create namespace
kubectl apply -f k8s/namespace.yaml

# 2. Create the Secret with your credentials
kubectl create secret generic cloud-ide-secrets `
  --namespace cloud-ide `
  --from-literal=MYSQL_ROOT_PASSWORD="root_secret" `
  --from-literal=MYSQL_USER="cloudide" `
  --from-literal=MYSQL_PASSWORD="cloudide_secret" `
  --from-literal=JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970" `
  --from-literal=EXECUTION_WORKER_SECRET="e4f9b2c8a1d743e09876543210fedcba876543210fedcba09876543210fedcba"
```

#### Linux / macOS / Bash:
```bash
# 1. Create namespace
kubectl apply -f k8s/namespace.yaml

# 2. Create the Secret with your credentials
kubectl create secret generic cloud-ide-secrets \
  --namespace cloud-ide \
  --from-literal=MYSQL_ROOT_PASSWORD="root_secret" \
  --from-literal=MYSQL_USER="cloudide" \
  --from-literal=MYSQL_PASSWORD="cloudide_secret" \
  --from-literal=JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970" \
  --from-literal=EXECUTION_WORKER_SECRET="e4f9b2c8a1d743e09876543210fedcba876543210fedcba09876543210fedcba"
```

### Step 3: Deploy Manifests via Kustomize

Apply all Kubernetes manifests:

```powershell
kubectl apply -k k8s/
```

### Step 4: Verify Deployment Status

Check that all pods, services, and PVCs are running and Ready:

```powershell
# Check Pods
kubectl get pods -n cloud-ide

# Check Services
kubectl get svc -n cloud-ide

# Check Persistent Volume Claims
kubectl get pvc -n cloud-ide

# Check Deployments
kubectl get deployments -n cloud-ide
```

Expected output:
```text
NAME                        READY   STATUS    RESTARTS   AGE
backend-xxxxxxxxxx-xxxxx    1/1     Running   0          1m
frontend-xxxxxxxxxx-xxxxx   1/1     Running   0          1m
mysql-xxxxxxxxxx-xxxxx      1/1     Running   0          1m
```

---

## 4. Accessing the Application

- **Frontend Web UI**: Open [http://localhost:30080](http://localhost:30080) in your web browser.
- **Backend Health Check (via Nginx)**: [http://localhost:30080/api/health](http://localhost:30080/api/health)
- **Execution Engine Health Check**: [http://localhost:30080/api/execution/health](http://localhost:30080/api/execution/health)

---

## 5. Starting the Execution Worker (Host)

In Phase 9.2, the Execution Worker continues running natively on the host to manage Docker sandboxes securely without exposing Docker sockets inside Kubernetes:

```powershell
cd execution-worker
$env:EXECUTION_WORKER_SECRET="e4f9b2c8a1d743e09876543210fedcba876543210fedcba09876543210fedcba"
mvn spring-boot:run
```

---

## 6. Logs & Troubleshooting

```powershell
# View Backend logs
kubectl logs -l app=backend -n cloud-ide -f

# View Frontend logs
kubectl logs -l app=frontend -n cloud-ide -f

# View MySQL logs
kubectl logs -l app=mysql -n cloud-ide -f

# Describe Pod for debugging events
kubectl describe pod -l app=backend -n cloud-ide
```

---

## 7. Teardown / Cleanup

To delete all Cloud IDE Kubernetes resources:

```powershell
kubectl delete -k k8s/
```

To delete the persistent MySQL volume claim as well:
```powershell
kubectl delete pvc mysql-pvc -n cloud-ide
```

---

## 8. Docker Compose vs Local Kubernetes

| Aspect | Docker Compose (`docker compose up`) | Local Kubernetes (`kubectl apply -k k8s/`) |
| :--- | :--- | :--- |
| **Frontend URL** | `http://localhost:5173` | `http://localhost:30080` |
| **Backend Port** | `8088` (Host port published) | `8088` (ClusterIP internal) |
| **MySQL Port** | `3306` (Internal container port) | `3306` (ClusterIP internal) |
| **Configuration** | `.env` / `docker-compose.yml` | `ConfigMap` / `Secret` |
| **Storage** | Named Docker volume `cloud_ide_mysql_data` | Kubernetes `PersistentVolumeClaim` (5Gi) |
| **Worker Location** | Host (`host.docker.internal:8090`) | Host (`host.docker.internal:8090`) |
