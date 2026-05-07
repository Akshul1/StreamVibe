<div align="center">

# 📺 StreamVibe

### Adaptive Byte-Range Video Streaming Engine

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Maven](https://img.shields.io/badge/Maven-3.9.11-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

<p align="center">
  StreamVibe is a production-ready backend for a video streaming platform, implementing <b>HTTP Range Requests (RFC 7233)</b> to stream videos in configurable byte chunks — enabling fast seeking, low memory overhead, and native browser/player compatibility. Built on Spring Boot 3, Java 21, PostgreSQL 16, and fully containerized with Docker Compose.
</p>

</div>

---

## 📋 Table of Contents

- [How Streaming Works](#-how-streaming-works)
- [Architecture](#️-architecture)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
  - [Option A — Docker Compose (Recommended)](#option-a--docker-compose-recommended)
  - [Option B — Run Locally](#option-b--run-locally)
- [Configuration](#-configuration)
- [API Reference](#-api-reference)
- [API Documentation — Swagger UI](#-api-documentation--swagger-ui)
- [Database Schema](#-database-schema)
- [Docker Details](#-docker-details)
- [Testing](#-testing)
- [Known Limitations & Roadmap](#-known-limitations--roadmap)
- [Contributing](#-contributing)
- [License](#-license)

---

## 📡 How Streaming Works

Traditional file downloads send the **entire file** before playback begins. Video players require **seeking** — jumping to any timestamp instantly. StreamVibe implements the HTTP Range standard (RFC 7233) to solve this:

```
Client (Browser / Video Player)                     StreamVibe Server
─────────────────────────────────                   ──────────────────────────────
GET /api/v1/video/stream/{id}               →
  Range: bytes=0-1048575                            Read 1 MB from disk at offset 0
                                            ←       206 Partial Content
                                                    Content-Range: bytes 0-1048575/52428800

[User seeks to 00:45]

GET /api/v1/video/stream/{id}               →
  Range: bytes=10485760-11534335                    Read 1 MB from disk at offset 10 MB
                                            ←       206 Partial Content
                                                    Content-Range: bytes 10485760-11534335/52428800
```

Each chunk is **1 MB** (`CHUNK_SIZE = 1024 * 1024`). If no `Range` header is sent, the full file is returned with `200 OK`. Invalid or out-of-bounds ranges return `416 Range Not Satisfiable`. This is the same mechanism used by YouTube, Netflix, and every major streaming platform.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       Client / Video Player                             │
│               (Browser <video> tag, VLC, Postman, curl)                 │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │  HTTP REST  (port 8080)
┌──────────────────────────────▼──────────────────────────────────────────┐
│                   VideoController  /api/v1/video                        │
│   POST /         GET /        GET /{id}        GET /stream/{id}         │
│  (Upload)    (List all)  (Get metadata)   (Byte-range stream)           │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────────┐
│                VideoService (interface) / VideoServiceImp               │
│    save()   get()   getByTitle()   getAll()   getVideoAsResource()      │
└──────────────┬───────────────────────────────────────┬──────────────────┘
               │  Spring Data JPA                      │  FileSystemResource
┌──────────────▼──────────────────┐      ┌─────────────▼──────────────────┐
│        VideoRepository          │      │      Local Disk  ./video/       │
│  JpaRepository<Video, String>   │      │   (host-mounted Docker volume)  │
│  + findByTitle(String title)    │      └────────────────────────────────┘
└──────────────┬──────────────────┘
               │
┌──────────────▼──────────────────┐
│   PostgreSQL 16  (port 5433)    │
│   DB: online_streaming_db       │
│   Tables: videos, courses       │
└─────────────────────────────────┘
```

---

## 🛠 Tech Stack

| Category | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 21 | Application runtime |
| Framework | Spring Boot | 3.3.0 | Web + auto-configuration |
| Web Layer | Spring MVC | 3.3.0 | REST controllers, multipart upload |
| Persistence | Spring Data JPA + Hibernate | 3.3.0 | ORM & repository abstraction |
| Database | PostgreSQL | 16 | Video and course metadata storage |
| API Docs | Springdoc OpenAPI (Swagger UI) | 2.5.0 | Interactive API explorer |
| Boilerplate | Lombok | Latest | `@Getter`, `@Setter`, `@Builder` |
| Containerization | Docker + Docker Compose | Latest | App + DB orchestration |
| Build Tool | Apache Maven (Wrapper) | 3.9.11 | Dependencies & packaging |
| Testing | Spring Boot Test / JUnit 5 | 3.3.0 | Context integration tests |

---

## ✅ Features

- **HTTP Range Streaming** — Serves videos in 1 MB chunks via `206 Partial Content`; supports `bytes=start-end`, `bytes=start-`, and `bytes=-suffix` range formats
- **Video Upload** — Accepts multipart uploads up to 1 GB; persists files to disk and metadata to PostgreSQL atomically
- **Video Metadata API** — Full read access for video records with title, description, MIME type, file path, and free-form JSON `meta` field
- **Course Entity** — Data model scaffolded and ready for future course-video association features
- **Custom Range Parser** — Handles all edge cases: missing end byte, out-of-bounds range capped to file size, malformed headers returning `416`
- **OpenAPI 3 Documentation** — All endpoints annotated with `@Operation`, `@Parameter`, `@Tag`; live Swagger UI at `/swagger-ui.html`
- **Multi-stage Dockerfile** — Build stage uses `maven:3.9.9-eclipse-temurin-21`; runtime stage uses slim `eclipse-temurin:21-jre` for a minimal image
- **Docker Compose** — One command brings up the app and `postgres:16` with a named persistent volume and a host-mounted video directory
- **Environment-variable-driven Config** — No hardcoded credentials; all DB connection details are injected via environment variables

---

## 📁 Project Structure

```
StreamVibe/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties         # Pins Maven 3.9.11
│
├── src/
│   ├── main/
│   │   ├── java/com/stream/app/
│   │   │   ├── OnlineStreamingPlatformApplication.java  # @SpringBootApplication entry point
│   │   │   │
│   │   │   ├── config/
│   │   │   │   └── OpenApiConfig.java        # Swagger title / version / description bean
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   └── VideoController.java      # REST endpoints + Range streaming logic
│   │   │   │                                 # CHUNK_SIZE = 1 MB, parseRange() helper
│   │   │   │
│   │   │   ├── entity/
│   │   │   │   ├── Video.java                # @Entity → videos table
│   │   │   │   └── Course.java               # @Entity → courses table
│   │   │   │
│   │   │   ├── payload/
│   │   │   │   └── CustomMessage.java        # Generic success/error response DTO
│   │   │   │
│   │   │   ├── repositories/
│   │   │   │   └── VideoRepository.java      # JpaRepository + findByTitle()
│   │   │   │
│   │   │   └── service/
│   │   │       ├── VideoService.java         # Interface: save, get, getAll, getVideoAsResource
│   │   │       └── imp/
│   │   │           └── VideoServiceImp.java  # @PostConstruct dir init, file I/O, DB ops
│   │   │
│   │   └── resources/
│   │       └── application.properties        # All config — fully env-var driven
│   │
│   └── test/
│       └── java/com/stream/app/
│           └── OnlineStreamingPlatformApplicationTests.java  # contextLoads() smoke test
│
├── .dockerignore                             # Excludes target/, .git, logs from image
├── .gitignore                                # Excludes build output, video files, IDE dirs
├── Dockerfile                                # Multi-stage: maven:3.9.9 → temurin:21-jre
├── docker-compose.yml                        # Services: app (8080) + db postgres:16 (5433)
├── mvnw / mvnw.cmd                           # Maven wrapper (Unix + Windows)
├── pom.xml                                   # Dependencies, plugins, Java 21 target
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Option A (Docker) | Option B (Local) |
|---|---|---|
| Docker Engine + Docker Compose | ✅ Required | ✗ |
| JDK 21+ | ✗ | ✅ Required |
| PostgreSQL 16 | ✗ (bundled) | ✅ Required |
| Git | ✅ | ✅ |

---

### Option A — Docker Compose (Recommended)

The fastest way to run the full stack with zero local setup beyond Docker.

**1. Clone**

```bash
git clone https://github.com/Akshul1/StreamVibe.git
cd StreamVibe
```

**2. Start all services**

```bash
docker compose up --build
```

This will:
- Build the app image from the multi-stage `Dockerfile`
- Pull and start `postgres:16` on port `5433`
- Start the Spring Boot app on port `8080`
- Create persistent volume `pgdata` for the database
- Mount `./video` on your host to `/app/video` in the container (uploaded files survive restarts)

**3. Verify**

```
http://localhost:8080/swagger-ui.html
```

**4. Stop**

```bash
docker compose down        # Stop, keep data volume
docker compose down -v     # Stop + delete all data
```

---

### Option B — Run Locally

**1. Clone**

```bash
git clone https://github.com/Akshul1/StreamVibe.git
cd StreamVibe
```

**2. Create the PostgreSQL database**

```sql
CREATE DATABASE online_streaming_db;
-- If using user 'postgres' with password 'root' (dev only):
ALTER USER postgres WITH PASSWORD 'root';
```

**3. Export environment variables**

```bash
# Unix / macOS
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/online_streaming_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=root

# Windows PowerShell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/online_streaming_db"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="root"
```

**4. Build and run**

```bash
# Unix / macOS
./mvnw clean spring-boot:run

# Windows
mvnw.cmd clean spring-boot:run
```

The app starts on `http://localhost:8080`. The `./video/` directory is created automatically on first startup via `@PostConstruct` in `VideoServiceImp`.

---

## ⚙️ Configuration

`src/main/resources/application.properties` — all sensitive values come from environment variables:

```properties
spring.application.name=OnlineStreamingPlatform

# Database — injected via environment variables
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# File Upload (max 1 GB)
spring.servlet.multipart.max-file-size=1000MB
spring.servlet.multipart.max-request-size=1000MB

# Video storage directory (relative to working dir, or absolute path)
files.video=video/

# Swagger UI
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.operationsSorter=method
```

| Variable | Description | Docker Compose Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://db:5432/online_streaming_db` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `root` |

> ⚠️ The default password `root` is for local development only. Use a strong password and a secrets manager (Docker Secrets, AWS Secrets Manager, etc.) in any deployed environment.

---

## 📖 API Reference

**Base URL:** `http://localhost:8080/api/v1/video`

All endpoints allow cross-origin requests (`@CrossOrigin("*")`).

---

### `POST /api/v1/video` — Upload a video

Uploads a video file and its metadata. A UUID is assigned server-side.

**Content-Type:** `multipart/form-data`

| Field | Type | Required | Description |
|---|---|---|---|
| `file` | `MultipartFile` | Yes | The video file (any MIME type) |
| `title` | `String` | Yes | Display title |
| `description` | `String` | Yes | Text description |

**Responses:**

| Status | Body | Description |
|---|---|---|
| `201 Created` | `Video` JSON | Upload successful |
| `500 Internal Server Error` | `CustomMessage { message, success }` | Upload failed |

**curl example:**

```bash
curl -X POST http://localhost:8080/api/v1/video \
  -F "file=@/path/to/demo.mp4" \
  -F "title=Demo Video" \
  -F "description=A streaming demo"
```

**Sample response:**

```json
{
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Demo Video",
  "description": "A streaming demo",
  "contentType": "video/mp4",
  "meta": null,
  "filePath": "video/demo.mp4"
}
```

---

### `GET /api/v1/video` — List all videos

Returns metadata for every video in the database.

**Response:** `200 OK` — `List<Video>`

```bash
curl http://localhost:8080/api/v1/video
```

---

### `GET /api/v1/video/{videoId}` — Get video metadata

Returns metadata for a single video.

**Path variable:** `videoId` (UUID string assigned at upload)

| Status | Description |
|---|---|
| `200 OK` | Returns `Video` JSON |
| `500 Internal Server Error` | No video found with that ID |

```bash
curl http://localhost:8080/api/v1/video/550e8400-e29b-41d4-a716-446655440000
```

---

### `GET /api/v1/video/stream/{videoId}` — Stream a video

Streams video bytes. Fully supports HTTP Range requests for seeking.

**Path variable:** `videoId`

**Optional request header:** `Range: bytes=<start>-<end>`

| Status | Description |
|---|---|
| `200 OK` | No `Range` header — full file returned |
| `206 Partial Content` | Valid range — 1 MB (or less) chunk returned |
| `416 Range Not Satisfiable` | Invalid or out-of-bounds range |
| `500 Internal Server Error` | File missing from disk or I/O failure |

**Response headers (206):**

```
Content-Type:   video/mp4
Content-Range:  bytes 0-1048575/52428800
Content-Length: 1048576
Accept-Ranges:  bytes
```

**curl examples:**

```bash
# First 1 MB chunk
curl -H "Range: bytes=0-1048575" \
     http://localhost:8080/api/v1/video/stream/{videoId} \
     --output chunk1.mp4

# Seek to 10 MB
curl -H "Range: bytes=10485760-11534335" \
     http://localhost:8080/api/v1/video/stream/{videoId} \
     --output chunk2.mp4
```

**HTML embed — browser handles seeking automatically:**

```html
<video controls width="800">
  <source src="http://localhost:8080/api/v1/video/stream/{videoId}" type="video/mp4">
</video>
```

---

## 🔍 API Documentation — Swagger UI

Once the app is running:

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive Swagger UI |
| `http://localhost:8080/api-docs` | Raw OpenAPI 3 JSON spec |

Configured in `OpenApiConfig.java`:

```
Title:   Online Streaming Platform API  
Version: 1.0  
Tag:     Videos — Upload, list, and stream videos
```

---

## 🗄️ Database Schema

Hibernate manages schema via `ddl-auto=update`. Underlying DDL:

**`videos`**

```sql
CREATE TABLE videos (
    video_id    VARCHAR(255) PRIMARY KEY,   -- UUID, set at upload
    title       VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    contentType VARCHAR(255),               -- MIME type, e.g. video/mp4
    meta        TEXT,                       -- Free-form JSON metadata
    file_path   TEXT NOT NULL               -- Absolute path on disk
);
```

**`courses`** (scaffolded, ready for future course-video association)

```sql
CREATE TABLE courses (
    courseId    VARCHAR(255) PRIMARY KEY,
    courseName  VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    type        VARCHAR(255),
    active      BOOLEAN,
    status      VARCHAR(255),
    duration    INTEGER,
    price       DOUBLE PRECISION
);
```

---

## 🐳 Docker Details

### Dockerfile walkthrough

```dockerfile
# Stage 1: Build — full Maven + JDK image
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run — slim JRE only (no Maven, no source)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

The runtime image contains only the JRE and the fat JAR, keeping the image lean.

### docker-compose.yml service summary

| Service | Container | Image | Ports | Notes |
|---|---|---|---|---|
| `db` | `stream-postgres` | `postgres:16` | `5433→5432` | Volume: `pgdata` (persistent) |
| `app` | `stream-app` | Built locally | `8080→8080` | Depends on `db`; volume: `./video→/app/video` |

### Useful Docker commands

```bash
# Start everything
docker compose up --build

# Run in background
docker compose up -d --build

# Live app logs
docker compose logs -f app

# Rebuild only the app after code changes
docker compose up -d --build app

# Open a psql session in the running DB container
docker exec -it stream-postgres psql -U postgres -d online_streaming_db

# Stop and keep data
docker compose down

# Full reset (removes database volume)
docker compose down -v
```

---

## 🧪 Testing

```bash
# Run all tests (Unix)
./mvnw test

# Run all tests (Windows)
mvnw.cmd test

# Run a specific test class
./mvnw test -Dtest=OnlineStreamingPlatformApplicationTests
```

The included `contextLoads()` smoke test verifies the full Spring application context starts with all beans wired correctly.

For tests that need the database, add `src/test/resources/application.properties` to use an in-memory H2 substitute:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
```

And add the H2 test dependency to `pom.xml`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 🗺 Known Limitations & Roadmap

### Current limitations

| Area | Detail |
|---|---|
| Error handling | No global `@ControllerAdvice` — `RuntimeException` bubbles up as a `500` with a stack trace |
| Typo in DTO | `CustomMessage.sucess` field has a missing 's' — should be `success` |
| No authentication | All endpoints are open; there is no Spring Security or JWT layer |
| Filename collisions | Two files with the same name silently overwrite each other (`REPLACE_EXISTING`) |
| Streaming memory | `readNBytes((int) length)` allocates the whole chunk on the heap; large ranges impact GC |
| No pagination | `GET /` returns all video records in a single query |

### Roadmap

- [ ] Global `@ControllerAdvice` with structured JSON error responses
- [ ] Spring Security + JWT authentication and role-based access control
- [ ] HLS transcoding — FFmpeg integration for `.m3u8` + `.ts` adaptive streaming
- [ ] UUID-prefixed filenames at upload to eliminate collision risk
- [ ] Course ↔ Video many-to-many association
- [ ] Paginated `GET /api/v1/video` with `Pageable`
- [ ] GitHub Actions CI pipeline — build and test on every push
- [ ] JaCoCo test coverage reporting

---

## 🤝 Contributing

Contributions are welcome. Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit using [Conventional Commits](https://www.conventionalcommits.org/):

   ```
   feat:     new feature
   fix:      bug fix
   refactor: code change without functional effect
   docs:     documentation only
   test:     adding or updating tests
   chore:    build, dependencies, CI
   ```

4. Push: `git push origin feature/your-feature-name`
5. Open a Pull Request against `main`

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Akshul Jamwal**
- GitHub: [@Akshul1](https://github.com/Akshul1)
- Email: akshuljamwal@gmail.com

---

<div align="center">
  <sub>Built with Spring Boot 3 · Java 21 · PostgreSQL 16 · Docker</sub>
</div>
