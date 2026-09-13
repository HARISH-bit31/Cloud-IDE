# Cloud IDE — Execution Worker Architecture (Phase 9.2)

This document outlines the security boundary, current local development setup, and future production architecture for untrusted code execution in Cloud IDE.

---

## 1. Current Local Development Architecture

```
                               Kubernetes Cluster
                             (Namespace: cloud-ide)
                                        │
           ┌────────────────────────────┼────────────────────────────┐
           │                            │                            │
     Frontend Pod                 Backend Pod                    MySQL Pod
     (Nginx/React)            (Spring Boot Java 21)             (MySQL 8.0)
     NodePort 30080             ClusterIP 8088                 ClusterIP 3306
           │                            │                            │
           │ proxy /api                 │ JDBC                       ▼
           └───────────────────────────►│                        mysql-pvc
                                        │                          (5Gi)
                                        │ HTTP (Mutual Worker Secret)
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

### Why the Worker Remains on the Host in Local Kubernetes
1. **Zero Docker Socket Exposure in Kubernetes**: Mounting `/var/run/docker.sock` into a Kubernetes pod gives root-equivalent control over the host node. Keeping the worker outside Kubernetes guarantees that web application pods have **no Docker access**.
2. **No Privileged Pods**: The backend and frontend pods run with strictly unprivileged security contexts (`runAsNonRoot: true`, `allowPrivilegeEscalation: false`).
3. **Dedicated Security Boundary**: Untrusted user code is executed in isolated ephemeral Docker containers on the host, separated from Kubernetes cluster networking and storage.
4. **Development Bridge (`host.docker.internal`)**: Facilitates clean HTTP communication from the Kubernetes backend pod to the host worker during local development.

---

## 2. Sandbox Security Controls & Isolation Matrix

Every code execution initiated by the Execution Worker enforces the following security boundaries:

| Security Control | Parameter | Purpose |
| :--- | :--- | :--- |
| **Ephemeral Container** | `--rm` | Container is automatically destroyed upon process exit |
| **Network Isolation** | `--network none` | Disables network stack; prevents outbound sockets, internet access, SSRF, and LAN scanning |
| **Memory Constraint** | `--memory 256m` | Prevents OOM starvation on host |
| **CPU Constraint** | `--cpus 1.0` | Prevents single execution from consuming all CPU cores |
| **Process / Fork Limit** | `--pids-limit 64` | Protection against fork bombs (`:(){ :\|:& };:`) |
| **Execution Timeout** | `10s Watchdog` | ScheduledExecutorService forcibly kills loops (exit code 124) |
| **Output Ceiling** | `64 KB` | Truncates stdout/stderr buffers to prevent memory overflow |
| **Non-Root Execution** | `USER sandboxuser` | Prevents container root privilege escalation |
| **Workspace Isolation** | Ephemeral bind-mount | Dedicated temporary directory (`cloudide-exec-<uuid>`) deleted in `finally` block |
| **Credential Isolation** | Zero secret injection | No JWT tokens, DB passwords, or worker secrets are passed into execution containers |
| **Worker Authentication** | `X-Execution-Worker-Secret` | Rejects unauthenticated requests (`401 Unauthorized`) |
| **Concurrency Ceiling** | `4 concurrent` | Rejects excess requests with `QUEUE_FULL` when busy |

---

## 3. Communication & Failure Handling

- **Authentication**: All requests between the Backend and Worker require the symmetric `X-Execution-Worker-Secret` header.
- **Fail-Fast Validation**: Backend and Worker fail startup immediately if the secret is missing or blank.
- **Clean Failure Modes**:
  - Worker offline $\rightarrow$ Backend returns `503` / clean JSON (`status: "SYSTEM_ERROR"`, `"stderr": "Code execution service is temporarily unavailable."`).
  - Timeout $\rightarrow$ Worker terminates container, returns `status: "TIMEOUT"` (exit code `124`).
  - Queue Full $\rightarrow$ Worker returns `status: "QUEUE_FULL"`.
  - Secret leakage prevention $\rightarrow$ No secrets or internal stack traces are returned to the client or written to logs.

---

## 4. Future Production Architecture (Phase 9.3+)

In a multi-tenant cloud deployment (e.g. AWS EKS / GCP GKE), `host.docker.internal` is replaced by dedicated, hardened execution worker nodes:

```
                      Kubernetes Web Tier
                    (Frontend & Backend Pods)
                               │
                               ▼
                       Execution Queue
                    (RabbitMQ / SQS / Redis)
                               │
                               ▼
                 Dedicated Execution Scheduler
                               │
          ┌────────────────────┴────────────────────┐
          ▼                                         ▼
   Worker Node Group 1                       Worker Node Group 2
 (Hardened Linux Kernel)                   (Hardened Linux Kernel)
          │                                         │
          ▼                                         ▼
   MicroVM / Sandbox Runtime                 MicroVM / Sandbox Runtime
   (Firecracker / gVisor runsc)              (Firecracker / gVisor runsc)
          │                                         │
          ▼                                         ▼
  Untrusted Execution                       Untrusted Execution
```

### Key Production Enhancements
1. **Asynchronous Job Queue**: Decouples HTTP request lifecycle from code execution execution bursts.
2. **MicroVM / Kernel Isolation**: Uses virtualization-based isolation (AWS Firecracker microVMs or Google gVisor user-space kernel `runsc`) instead of shared host kernel containers.
3. **Dedicated Node Pools with Taints & Tolerations**: Untrusted code runs on dedicated worker nodes isolated in private subnets with no access to the Kubernetes control plane or databases.
4. **Auto-Scaling Worker Nodes**: Horizontal Pod Autoscaling (HPA) and Karpenter/Cluster Autoscaler scale execution nodes on demand based on queue depth.
