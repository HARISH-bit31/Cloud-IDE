# Cloud IDE — Browser-Based Online Code Execution Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-Sandboxed%20Execution-2496ED.svg)](https://www.docker.com/)
[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)

A modern, high-performance browser-based IDE featuring real-time sandboxed code execution across **Java 21 LTS**, **Python 3.12**, **C23**, and **C++20** with interactive standard input (stdin) streaming, robust memory/CPU security constraints, a dedicated **Execution Worker** service, and persistent MySQL workspace storage.

---

## 🏗️ Architecture Overview

```
                      Browser Client
                            │
               ┌────────────┴────────────┐
               ▼                         ▼
         Frontend (5173)           Direct API (8088)
         Nginx + React SPA         Spring Boot REST
               │                         │
               └────────────┬────────────┘
                            │ /api/
                            ▼
                  Spring Boot Backend (8088)
                  • JWT Auth & Security
                  • Project & File CRUD
                  • Execution Ownership Authorization
                  • ExecutionWorkerClient (HTTP)
                            │
               ┌────────────┴──────────────────────────┐
               ▼                                       ▼
         MySQL 8.0 (3306)                   Execution Worker (8090)
         • Users & Auth                     • X-Execution-Worker-Secret
         • Projects & Files                 • Max Concurrency Throttling (4)
         • Named Volume                     • Active Session Management
                                            • Direct Stdin Piping
                                                       │
                                                       ▼
                                             Docker Desktop Engine
                                             • Ephemeral Sandboxes (--rm)
                                             • Isolated Network (--network none)
                                             ├── Java 21 (OpenJDK 21)
                                             ├── Python 3.12 (Python 3.12)
                                             ├── C (GCC 13 / Alpine)
                                             └── C++ (G++ 13 / Alpine)
```

---

## 🚀 Quick Start with Docker Compose & Execution Worker

The Cloud IDE infrastructure runs via Docker Compose with the dedicated **Execution Worker** running natively on the development host for secure Docker daemon access.

### 1. Prerequisites
- [Docker Desktop](https://docs.docker.com/get-docker/) running on Windows / macOS / Linux.
- Java 21+ & Maven 3.9+ installed on your host machine.

### 2. Environment Configuration
Copy the template environment file:
```bash
cp .env.example .env
```

### 3. Start the Application Stack

#### Terminal 1 — Start Docker Compose Infrastructure:
```bash
docker compose up --build -d
```
*Starts MySQL, Spring Boot Backend (connected via internal network), and React Frontend (Nginx).*

#### Terminal 2 — Start the Secure Execution Worker:
```bash
cd execution-worker
mvn spring-boot:run
```
*Starts the worker on port `8090` with direct access to the local Docker Engine.*

### 4. Verify Services
- **Frontend Web IDE**: [http://localhost:5173](http://localhost:5173)
- **Backend Health Check**: [http://localhost:8088/api/execution/health](http://localhost:8088/api/execution/health)
- **Direct API Root**: [http://localhost:8088/api](http://localhost:8088/api)
- **Worker Health Check**: `GET http://localhost:8090/internal/health` (requires `X-Execution-Worker-Secret: cloudide_worker_secret`)

### 5. Stop the Application Stack
```bash
docker compose down
```

---

## 🛡️ Security & Execution Worker Model

### Separation of Concerns
1. **Application Backend**:
   - Handles user authentication (JWT), project/file CRUD, and session ownership.
   - Generates unpredictable UUIDs for execution sessions and maps `executionId` $\leftrightarrow$ `userId`.
   - Rejects unauthorized users attempting to send stdin or stop another user's execution (`403 Forbidden`).
   - Does **NOT** mount `docker.sock` or control Docker directly.
2. **Execution Worker**:
   - An internal microservice listening on port `8090`.
   - Authenticates backend requests via header `X-Execution-Worker-Secret`.
   - Manages container lifecycles, interactive stdin streams, timeout enforcement, output buffering, and cleanup.
   - Enforces concurrency limits (`execution.max-concurrent=4`) returning `QUEUE_FULL` when busy.
3. **Docker Sandboxes**:
   - Ephemeral containers created per execution.
   - `--network none`: Full network isolation (blocking outbound sockets, internet access, and SSRF).
   - `--memory 256m`: Memory ceiling.
   - `--cpus 1.0`: Single core CPU limit.
   - `--pids-limit 64`: Fork bomb protection.
   - `10s Watchdog`: Kills runaway loops with exit code 124.
   - Guaranteed cleanup in `finally` blocks & shutdown hooks (`docker rm -f`).

### Development vs. Production Architecture
- **Development Setup (Current)**:
  - Spring Boot Backend runs in a Compose container and connects to the host worker via `http://host.docker.internal:8090`.
  - The worker runs directly on the developer host machine with access to Docker Desktop.
  - No insecure Docker TCP sockets (`tcp://0.0.0.0:2375`) are opened.
- **Production Architecture (Future AWS / Kubernetes)**:
  - Backend and Worker run as separate isolated Kubernetes Deployments.
  - Worker runs on dedicated execution worker nodes with microVM / rootless sandboxing runtimes (e.g., Firecracker, gVisor, or Kata Containers) inside VPC-isolated subnets.

---

## 🧪 Automated Testing & Verification

### Run Backend Tests (15 unit & security tests)
```bash
cd backend
mvn clean test
```

### Run Worker Tests (6 unit & security tests)
```bash
cd execution-worker
mvn clean test
```

### Run Frontend Production Build
```bash
npm run build
```

---

---

## ☸️ Local Kubernetes Deployment (Phase 9.1)

Cloud IDE supports local deployment to **Docker Desktop Kubernetes** using Kubernetes manifests located in the `k8s/` directory.

### 1. Prerequisites
- Docker Desktop with **Kubernetes enabled**.
- `kubectl` CLI configured to `docker-desktop` context.

### 2. Quick Deploy
```powershell
# 1. Build local container images
docker build -t cloud-ide-backend:latest ./backend
docker build -t cloud-ide-frontend:latest .

# 2. Create namespace & secret
kubectl apply -f k8s/namespace.yaml
kubectl create secret generic cloud-ide-secrets `
  --namespace cloud-ide `
  --from-literal=MYSQL_ROOT_PASSWORD="root_secret" `
  --from-literal=MYSQL_USER="cloudide" `
  --from-literal=MYSQL_PASSWORD="cloudide_secret" `
  --from-literal=JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970" `
  --from-literal=EXECUTION_WORKER_SECRET="e4f9b2c8a1d743e09876543210fedcba876543210fedcba09876543210fedcba"

# 3. Apply manifests
kubectl apply -k k8s/

# 4. Forward port to access the Frontend UI (or use NodePort 30080)
kubectl port-forward svc/frontend 30080:80 -n cloud-ide
```

Access the application in your browser at: **`http://localhost:30080`**

For complete details, see [`k8s/README.md`](file:///c:/Users/Welcome/Documents/projects/Cloud-IDE/k8s/README.md).

---

## ⚙️ Environment Variables Reference

| Variable | Default (Local) | Compose Default | Kubernetes | Description |
|---|---|---|---|---|
| `DB_HOST` | `localhost` | `mysql` | `mysql` | MySQL hostname |
| `DB_PORT` | `3306` | `3306` | `3306` | MySQL port |
| `DB_NAME` | `cloud_ide` | `cloud_ide` | `cloud_ide` | Database name |
| `DB_USERNAME` | `root` | `cloudide` | Secret (`MYSQL_USER`) | Database username |
| `DB_PASSWORD` | `root` | `cloudide_secret` | Secret (`MYSQL_PASSWORD`) | Database password |
| `JWT_SECRET` | *Dev default* | *Configured* | Secret (`JWT_SECRET`) | Base64/Hex JWT secret key (256-bit+) |
| `JWT_EXPIRATION` | `86400000` | `86400000` | `86400000` | Token expiration time in milliseconds |
| `EXECUTION_WORKER_URL` | `http://localhost:8090` | `http://host.docker.internal:8090` | `http://host.docker.internal:8090` | URL for Execution Worker |
| `EXECUTION_WORKER_SECRET` | *Must configure* | *Must configure* | Secret (`EXECUTION_WORKER_SECRET`) | Shared secret header for worker auth |
| `SERVER_PORT` | `8088` | `8088` | `8088` | Backend server port |
| `FRONTEND_PORT` | `5173` | `5173` | `30080` | Frontend web port |

