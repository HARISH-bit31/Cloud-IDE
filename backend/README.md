# Cloud IDE — Spring Boot Backend API

Clean, maintainable, and robust Spring Boot REST API backend for the **Cloud IDE — Browser-Based Online Code Execution Platform**.

---

## 🛠️ Technology Stack

* **Language:** Java 21 / 25
* **Framework:** Spring Boot 3.4.3
* **Security & Auth:** Spring Security 6.x + JWT (HMAC-SHA256) + BCrypt
* **Execution Sandboxing:** Docker Engine with `--network none`, 256MB RAM limit, 1.0 CPU limit, pids-limit 64
* **Sandbox Runtimes:**
  - Java 21 LTS (`eclipse-temurin:21-jdk-alpine`)
  - Python 3.12 (`python:3.12-alpine`)
  - C23 GCC 13.2 (`alpine:3.20` + `gcc musl-dev`)
  - C++20 G++ 13.2 (`alpine:3.20` + `g++ musl-dev`)
* **Data Access:** Spring Data JPA / Hibernate
* **Database:** MySQL 8.0+ (with optional H2 in-memory mode for fast local development)
* **Build Tool:** Apache Maven 3.9+
* **Validation:** Jakarta Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Email`)
* **Logging:** SLF4J / Logback

---

## 📂 Project Architecture

```
backend/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/cloudide/cloudide/
    │   │   ├── CloudIdeApplication.java         # Spring Boot entry point
    │   │   ├── config/
    │   │   │   ├── CorsConfig.java               # Explicit CORS for Vite/React (5173)
    │   │   │   └── DataInitializer.java          # Seed default developer user & starter projects
    │   │   ├── controller/
    │   │   │   ├── HealthController.java         # GET /api/health
    │   │   │   ├── UserController.java           # User management endpoints
    │   │   │   ├── ProjectController.java        # Project CRUD endpoints
    │   │   │   └── FileController.java           # Nested file CRUD endpoints
    │   │   ├── service/
    │   │   │   ├── UserService.java              # User business logic
    │   │   │   ├── ProjectService.java           # Project lifecycle & starter scaffolding
    │   │   │   └── FileService.java              # File CRUD & language detection
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java           # JPA repository for User
    │   │   │   ├── ProjectRepository.java        # JPA repository for Project
    │   │   │   └── FileRepository.java           # JPA repository for File
    │   │   ├── entity/
    │   │   │   ├── User.java                     # User JPA entity
    │   │   │   ├── Project.java                  # Project JPA entity (ManyToOne User)
    │   │   │   └── File.java                     # File JPA entity (ManyToOne Project)
    │   │   ├── dto/
    │   │   │   ├── HealthResponse.java           # Health check payload
    │   │   │   ├── UserRequest.java / UserResponse.java
    │   │   │   ├── ProjectRequest.java / ProjectResponse.java
    │   │   │   ├── FileRequest.java / FileResponse.java
    │   │   │   └── ErrorResponse.java            # Uniform error payload
    │   │   ├── enums/
    │   │   │   └── ProgrammingLanguage.java      # JAVA, PYTHON, C, CPP
    │   │   ├── exception/
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   ├── BadRequestException.java
    │   │   │   └── GlobalExceptionHandler.java   # @RestControllerAdvice
    │   │   └── mapper/
    │   │       └── DtoMapper.java                # Entity <-> DTO converters
    │   └── resources/
    │       ├── application.properties           # Default MySQL configuration
    │       └── application-dev.properties       # Fast in-memory H2 development profile
    └── test/
        └── java/com/cloudide/cloudide/
            ├── CloudIdeApplicationTests.java    # Spring context loading test
            └── controller/ApiControllerTests.java # Full MockMvc integration tests
```

---

## ⚙️ Configuration & Database Setup

### 1. MySQL Setup (Default Profile)
Ensure MySQL is running on your machine.
1. Create the database (optional, Spring Boot creates it automatically if permissions allow):
   ```sql
   CREATE DATABASE IF NOT EXISTS cloud_ide;
   ```
2. Set environment variables (or rely on default localhost credentials):
   | Variable | Description | Default |
   |---|---|---|
   | `DB_HOST` | MySQL hostname | `localhost` |
   | `DB_PORT` | MySQL port | `3306` |
   | `DB_NAME` | Database schema name | `cloud_ide` |
   | `DB_USERNAME` | Database username | `root` |
   | `DB_PASSWORD` | Database password | `root` |
   | `SERVER_PORT` | HTTP Server listening port | `8088` |

### 2. Fast Development Mode (In-Memory H2)
To run instantly without needing a running MySQL service, activate the `dev` profile:
```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```
H2 Web Console is accessible at: `http://localhost:8088/h2-console`
* **JDBC URL:** `jdbc:h2:mem:cloud_ide`
* **User:** `sa`
* **Password:** *(empty)*

---

## 🚀 Running the Application

### Using Maven:
```bash
cd backend
mvn spring-boot:run
```

### Running Tests:
```bash
cd backend
mvn clean test
```

---

## 📡 API Endpoints Reference

### 1. Health Check
* **`GET /api/health`**
  * **Response `200 OK`:**
    ```json
    {
      "status": "UP",
      "service": "Cloud IDE Backend",
      "timestamp": "2026-09-12T10:38:53.175"
    }
    ```

---

### 2. Projects API
* **`GET /api/projects`** — List all projects
  * **Response `200 OK`:**
    ```json
    [
      {
        "id": 1,
        "name": "Java Practice",
        "description": "Cloud sandbox JDK 21 LTS arithmetic and scanner pipelines",
        "language": "JAVA",
        "userId": 1,
        "userName": "Alex Developer",
        "fileCount": 3,
        "files": [
          { "id": 1, "filename": "Main.java", "language": "JAVA", "projectId": 1 },
          { "id": 2, "filename": "Calculator.java", "language": "JAVA", "projectId": 1 },
          { "id": 3, "filename": "README.md", "language": "JAVA", "projectId": 1 }
        ],
        "createdAt": "2026-09-12T10:38:27.075",
        "updatedAt": "2026-09-12T10:38:27.075"
      }
    ]
    ```

* **`GET /api/projects/{id}`** — Get project by ID
* **`POST /api/projects`** — Create a new project (auto-scaffolds starter code)
  * **Request Body:**
    ```json
    {
      "name": "Algorithms Lab",
      "description": "C++ competitive algorithms",
      "language": "CPP"
    }
    ```
  * **Response `201 Created`**
* **`PUT /api/projects/{id}`** — Update project metadata
* **`DELETE /api/projects/{id}`** — Delete project and its files (`204 No Content`)

---

### 3. Files API
* **`GET /api/projects/{projectId}/files`** — List all files in a project
* **`GET /api/projects/{projectId}/files/{fileId}`** — Get file by ID
* **`POST /api/projects/{projectId}/files`** — Add a file to a project
  * **Request Body:**
    ```json
    {
      "filename": "BinarySearch.cpp",
      "content": "#include <iostream>\nint main() { return 0; }",
      "language": "CPP"
    }
    ```
  * **Response `201 Created`**
* **`PUT /api/projects/{projectId}/files/{fileId}`** — Update file content/name
* **`DELETE /api/projects/{projectId}/files/{fileId}`** — Delete file (`204 No Content`)

---

### 4. Users API
* **`GET /api/users`** — List users
* **`GET /api/users/{id}`** — Get user by ID
* **`POST /api/users`** — Register new user
  * **Request Body:**
    ```json
    {
      "name": "Alex Developer",
      "email": "alex.developer@cloud-ide.io",
      "password": "password123"
    }
    ```

---

## 🛡️ Error Handling
The backend implements `@RestControllerAdvice` returning standard error payloads:
```json
{
  "status": 404,
  "message": "Project not found with id: 99",
  "path": "/api/projects/99",
  "timestamp": "2026-09-12T10:33:56.311"
}
```

Validation errors (`400 Bad Request`):
```json
{
  "status": 400,
  "message": "Validation failed for one or more fields",
  "errors": [
    "Project name is required"
  ],
  "path": "/api/projects",
  "timestamp": "2026-09-12T10:33:56.311"
}
```

---

## 🌐 CORS Configuration
CORS is pre-configured in `CorsConfig.java` to allow requests from the React Vite frontend at `http://localhost:5173` with credentials and standard HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`).
