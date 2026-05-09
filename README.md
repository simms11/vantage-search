# Vantage Search

An AI-powered search system built for the Vantage WealthTech platform. Advisors can intelligently search across client records and documents using a combination of fuzzy-text matching and semantic vector search, secured behind JWT authentication with per-IP rate limiting.

## Architectural Decisions

1. **Hexagonal Architecture**
   Core domain logic is entirely isolated from infrastructure concerns. Switching from local Ollama models to OpenAI, or from PostgreSQL to Pinecone, requires zero changes to business logic.

2. **Asynchronous, Event-Driven AI**
   Document uploads instantly return `201 Created`. A `@TransactionalEventListener` fires after commit, generating AI summaries and `pgvector` embeddings in a background thread with automatic retry (3 attempts, linear backoff).

3. **Hybrid Search with RRF**
   - **Clients (Fuzzy):** PostgreSQL `pg_trgm` GIN indexes search names, descriptions, and emails simultaneously.
   - **Documents (Semantic):** `pgvector` HNSW indexes with `nomic-embed-text` embeddings — searching *"address proof"* retrieves documents containing *"utility bill"*.
   - Results from both sources are merged using **Reciprocal Rank Fusion (RRF)** into a single ranked list.

4. **Security**
   Stateless JWT authentication with per-IP rate limiting (Redis-backed, distributed across all instances). Tokens carry a `jti` claim and can be individually revoked via logout. Users are stored in PostgreSQL; the initial admin is bootstrapped from environment variables on first start.

5. **Graceful Degradation**
   If Ollama is unavailable, the API returns relational matches only. If Redis is unavailable, rate limiting fails open to preserve availability.

6. **Observability**
   - `CorrelationIdFilter` stamps every request with a `traceId`, propagated across async threads via MDC.
   - Structured JSON logs (logstash-logback-encoder) for production; human-readable format for the `dev` profile.
   - Prometheus metrics at `/actuator/prometheus`.

---

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local development/testing)
- Ollama running locally (`ollama run llama3` and `ollama pull nomic-embed-text`)

### 1. Configure Environment Variables

```bash
cp .env.example .env
```

Edit `.env` and fill in all values:

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `APP_JWT_SECRET` | Base64-encoded secret, min 32 bytes — generate: `openssl rand -base64 32` |
| `APP_ADMIN_PASSWORD` | `{bcrypt}`-prefixed hash — generate: `htpasswd -bnBC 10 "" pass \| tr -d ':\n'` |
| `APP_ADMIN_USERNAME` | Admin username (default: `admin`) |
| `REDIS_URL` | Redis connection URL (default: `redis://vantage_redis:6379`) |
| `SPRING_PROFILES_ACTIVE` | Leave empty for production; set to `dev` to seed sample data |

### 2. Start the Stack

```bash
docker-compose up -d --build
```

This starts PostgreSQL (pgvector + pg_trgm), Redis, and the application. Liquibase runs automatically on first start to create all tables and indexes.

### 3. Authenticate

All endpoints (except `/api/auth/login`) require a Bearer token.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "your_password"}'
```

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

Use this token in all subsequent requests: `Authorization: Bearer <token>`

### 4. Verify Health

```
GET http://localhost:8080/actuator/health
```

Returns `{"status": "UP"}` publicly. Full details (DB, Redis) require authentication.

---

## API Reference

Interactive docs (with auth support) available at: `http://localhost:8080/swagger-ui.html`

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | None | Obtain a JWT |
| `POST` | `/api/auth/logout` | Required | Revoke the current token immediately |

### Clients

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/clients` | Required | Create a new client |
| `GET` | `/api/clients/{id}` | Required | Get a client by ID |
| `DELETE` | `/api/clients/{id}` | Required | Delete a client and all associated documents |
| `POST` | `/api/clients/{id}/documents` | Required | Attach a document (triggers async AI indexing) |
| `GET` | `/api/clients/{id}/documents` | Required | List all documents for a client |

### Search

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/search?query=...&page=0&size=20` | Required | Hybrid search across clients and documents |

---

## Example Requests

### Create a Client

```http
POST /api/clients
Authorization: Bearer <token>
Content-Type: application/json

{
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@vantage.com",
    "description": "High net worth advisor",
    "social_links": ["https://linkedin.com/in/johndoe"]
}
```

```json
{
  "id": "e2f1b4c3-9a8b-4d7e-8f6a-5b4c3d2e1f0a",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@vantage.com",
  "description": "High net worth advisor",
  "socialLinks": ["https://linkedin.com/in/johndoe"],
  "score": 1.0,
  "explanation": "Newly created client"
}
```

### Attach a Document

```http
POST /api/clients/e2f1b4c3-9a8b-4d7e-8f6a-5b4c3d2e1f0a/documents
Authorization: Bearer <token>
Content-Type: application/json

{
    "title": "Address Verification",
    "content": "Please find attached the January utility bill for the London property."
}
```

The document is persisted immediately (201). AI summarisation and vector indexing happen in the background — the `summary` field updates asynchronously.

### Hybrid Search

```http
GET /api/search?query=address+proof
Authorization: Bearer <token>
```

```json
[
  {
    "type": "document",
    "id": "a1b2c3d4-e5f6-4a5b-8c7d-9e0f1a2b3c4d",
    "clientId": "e2f1b4c3-9a8b-4d7e-8f6a-5b4c3d2e1f0a",
    "title": "Address Verification",
    "summary": "• Utility bill provided.\n• Located in London.",
    "score": 0.0164,
    "explanation": "Semantic match via AI vector embeddings (Normalised via RRF)"
  }
]
```

### Logout

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

Returns `204 No Content`. The token is immediately invalidated — subsequent requests with it will be rejected.

---

## Rate Limiting

60 requests per IP per 60-second window. Backed by Redis — limits are shared across all instances. Returns `429 Too Many Requests` when exceeded.

---

## Testing

The test suite uses Testcontainers to spin up isolated PostgreSQL and Redis instances. WireMock stubs Ollama responses so no local GPU or LLM is required.

```bash
./mvnw test
```

Runs the full suite. Unit tests are instant; integration tests spin up Postgres and Redis via Testcontainers and require Docker.

---

## Tech Stack

| | |
|---|---|
| **Runtime** | Java 21, Spring Boot 3.3.4 |
| **AI** | Spring AI 1.0.0-M6, Ollama (llama3 + nomic-embed-text) |
| **Database** | PostgreSQL 16 with pgvector and pg_trgm |
| **Cache / Rate limiting** | Redis 7 |
| **Schema management** | Liquibase |
| **Security** | Spring Security, JJWT |
| **Observability** | Micrometer + Prometheus, logstash-logback-encoder |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Testing** | JUnit 5, Testcontainers, WireMock, Mockito |
| **Infrastructure** | Docker, Docker Compose |
