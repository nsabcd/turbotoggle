# TurboToggle 🚀

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java Version](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://docs.oracle.com/en/java/javase/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Coverage](https://img.shields.io/badge/coverage-80%25%20enforced-brightgreen.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**TurboToggle** is a high-performance, real-time feature flagging and targeted rollout platform designed as a light-weight, self-hosted alternative to LaunchDarkly. Built with **Spring Boot 3.3**, **Java 21 Virtual Threads**, **Redis Pub/Sub**, and **PostgreSQL/H2**, TurboToggle provides sub-5ms local evaluation latency and real-time streaming updates via Server-Sent Events (SSE).

---

## 🌟 Key Features

* **Multi-Variate & Boolean Flags**: Support for standard `BOOLEAN` toggles as well as `STRING`, `INTEGER`, and `JSON` multi-variate payloads.
* **Environment Segregation**: Isolated flag evaluations and configurations across `dev`, `stage`, and `prod` environments using SDK keys.
* **Complex Targeting Rules & Percentage Rollouts**:
    * **Individual Targeting**: Direct mapping of explicit User IDs to flag variations.
    * **Attribute Rule Matching**: Attribute clause evaluation (`EQUALS`, `IN`, `GREATER_THAN`, etc.).
    * **Deterministic Percentage Rollouts**: Uniform traffic hashing powered by **MurmurHash3**.
* **Real-Time Delivery & Zero-Latency Evaluation**:
    * **Local Evaluation**: In-memory evaluation inside client application SDKs (sub-5ms execution).
    * **Server-Sent Events (SSE)**: Immediate propagation of flag configuration updates within ~2 seconds.
    * **Fallback Safety**: Soft fallback defaults if network or central server connection fails.
* **Robust Enterprise Features**:
    * **Cache-Aside Architecture**: Multi-level cache with distributed locking fallback to prevent cache stampedes.
    * **Audit Logging**: Comprehensive mutation tracking recorded with user ID, timestamp, and state deltas.
    * **Optimistic Locking**: Schema validation and `@Version` control to prevent concurrent update conflicts.

---

## 🏗 System Architecture

TurboToggle is structured around a decoupled Control Plane and Data Streaming Engine to ensure high throughput and fault isolation.

```
┌────────────────────────────────────────────────────────┐
│                   Admin UI / Dashboard                 │
└─────────────────────────┬──────────────────────────────┘
                          │ REST API
┌─────────────────────────▼──────────────────────────────┐
│       Spring Boot Control Plane (Admin Service)        │
└─────────────────────────┬──────────────────────────────┘
                          │ DB Sync / Audit Logs
┌─────────────────────────▼──────────────────────────────┐
│             Database / Cache (PostgreSQL/Redis)        │
└─────────────────────────▲──────────────────────────────┘
                          │ Cache Read / PubSub
┌─────────────────────────┴──────────────────────────────┐
│       Spring WebFlux Streaming Engine (SSE Server)     │
└─────────────────────────┬──────────────────────────────┘
                          │ Real-time Stream (SSE)
┌─────────────────────────▼──────────────────────────────┐
│             Application SDKs (Java / Node / etc.)      │
└────────────────────────────────────────────────────────┘
```

---

## 📦 Project Structure

```text
turbotoggle/
├── turbotoggle-core/         # Rule evaluation engine, MurmurHash3 bucketing, core DTOs
├── turbotoggle-server/       # REST Control Plane, JPA storage, Redis Caching, SSE Streaming
└── turbotoggle-sdk/          # Java 21 Virtual-Thread powered SSE client & in-memory flag store
```

### Module Overview

| Module | Responsibilities |
| :--- | :--- |
| **`turbotoggle-core`** | Evaluation logic (`EvaluatorService`, `ClauseEvaluator`), deterministic bucketing (`HashingUtil`), and data models. |
| **`turbotoggle-server`** | Control plane REST APIs, environment configuration, JaCoCo coverage rules, Redis Pub/Sub, and SSE connections. |
| **`turbotoggle-sdk`** | Lightweight client-side SDK maintaining local cache via SSE stream (`SseStreamClient`, `InMemoryFlagStore`). |

---

## 🚀 Getting Started

### Prerequisites

* **Java JDK 21** or higher
* **Docker & Docker Compose** (for PostgreSQL and Redis)
* **Gradle 8.x** (wrapper provided)

### 1. Start Infrastructure Services

Spin up local Redis and PostgreSQL containers using Docker Compose:

```bash
cd turbotoggle-server
docker-compose up -d
```

This starts:
* **Redis 7** on port `6379`
* **PostgreSQL 16** on port `5432` (Database: `turbotoggle_stage`, User: `postgres`)

### 2. Build the Project

Build all modules and run unit & integration test suites:

```bash
./gradlew build
```

---

## ⚙️ Configuration & Execution

The server supports multi-profile configurations (`dev`, `stage`, `prod`).

### Running Locally (Dev Profile)

By default, the server uses the `dev` profile with an in-memory **H2 Database** and local Redis:

```bash
./gradlew :turbotoggle-server:bootRun --args='--spring.profiles.active=dev'
```

* **H2 Console**: Accessible at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:turbotoggledb`)

### Running with PostgreSQL (Stage Profile)

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=turbotoggle_stage
export DB_USER=postgres
export DB_PASSWORD=postgrespassword
export REDIS_HOST=localhost
export REDIS_PORT=6379

./gradlew :turbotoggle-server:bootRun --args='--spring.profiles.active=stage'
```

---

## 🔌 API Reference & Usage Examples

### 1. Create an Environment

```bash
POST /api/v1/environments
Content-Type: application/json

{
  "envKey": "staging",
  "name": "Staging Environment"
}
```

**Response (`201 Created`):**
```json
{
  "id": 1,
  "envKey": "staging",
  "name": "Staging Environment",
  "sdkKey": "sdk_a1b2c3d4e5f67890"
}
```

### 2. Create a Feature Flag

```bash
POST /api/v1/control/flags
X-User-Id: admin@turbotoggle.io
Content-Type: application/json

{
  "flagKey": "checkout-v2",
  "name": "New Checkout Flow",
  "description": "Beta Checkout UI",
  "flagType": "BOOLEAN"
}
```

### 3. Update Flag Targeting Rules & Rollouts

```bash
PUT /api/v1/control/flags/staging/checkout-v2/rules
X-User-Id: admin@turbotoggle.io
Content-Type: application/json

{
  "enabled": true,
  "defaultVariation": "false",
  "rules": [
    {
      "ruleId": "rule-1",
      "variation": "true",
      "clauses": [
        {
          "attribute": "country",
          "operator": "EQUALS",
          "values": ["US", "CA"]
        }
      ],
      "percentageRollouts": []
    }
  ],
  "individualTargets": {
    "user-10": "true"
  }
}
```

### 4. Evaluate Flag (Server-side Endpoint)

```bash
POST /api/v1/evaluate/checkout-v2
X-SDK-Key: sdk_a1b2c3d4e5f67890
Content-Type: application/json

{
  "entityId": "user-10",
  "attributes": {
    "country": "US"
  }
}
```

### 5. Stream Real-Time Rule Updates (SSE)

```bash
GET /api/v1/stream
X-SDK-Key: sdk_a1b2c3d4e5f67890
Accept: text/event-stream
```

---

## 🧪 Testing & Code Coverage

TurboToggle maintains high reliability through comprehensive test suites combining **JUnit 5**, **MockMvc**, **Testcontainers**, and **JaCoCo**.

### Run Test Suite

```bash
./gradlew test
```

### Generate Coverage Report

Code coverage thresholds (minimum 80%) are enforced via JaCoCo:

```bash
./gradlew jacocoTestReport jacocoTestCoverageVerification
```

Reports are generated at:
`turbotoggle-server/build/reports/jacoco/test/html/index.html`

### Test Suite Map

| Test Class | Focus |
| :--- | :--- |
| **`FeatureFlagControllerIntegrationTest`** | End-to-end lifecycle verification & rollout validation error cases. |
| **`EvaluationControllerTest`** | Evaluation API endpoints & batch fallback behavior. |
| **`FetchFlagServiceTest`** | Cache miss/hit resolution, concurrency, and distributed locking. |
| **`CacheInvalidationListenerTest`** | Cache eviction triggers on Redis Pub/Sub events. |
| **`SseEmitterRegistryTest`** | Client streaming connection lifecycle & timeout handling. |
| **`FlagPayloadJsonTest`** | Jackson serialization & deserialization verification. |

---

## 🗺 Roadmap & Next Steps

* [x] **Phase 1A: Verification & Core Testing** — Unit tests, Integration tests, and Testcontainers setup.
* [ ] **Phase 1B: JSON Parsing & Test Coverage Hardening** — Address object-to-JSON mapping edge cases and increase test coverage using **Plausible / Pitest** mutation testing or **Nullaway**.
* [ ] **Phase 2: System Infrastructure & Schemas** — Liquibase/Flyway DB migrations, PostgreSQL JSONB column optimizations.
* [ ] **Phase 3: Telemetry, Metrics & Analytics** — Prometheus/Micrometer metrics for flag evaluation counts and SSE client connections.
* [ ] **Phase 4: Production Readiness** — Rate limiting, API key rotation, and multi-region Redis replication.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
