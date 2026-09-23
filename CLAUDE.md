# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Job-application tech challenge: three independent Spring Boot microservices that work together,
sharing one local infrastructure stack defined in the root `docker-compose.yml`.

- Spring Boot **3.5.x** (the user asked for Spring 3; do not upgrade to Boot 4), Java 21, Maven.
- Each service is a **fully independent** Maven project (own `pom.xml`, `mvnw`, `Dockerfile`).
  There is no parent/aggregator pom — this was a deliberate choice, keep it that way.

## Services

| Folder                | Artifact             | Package                 | Port | SQS role                               |
|-----------------------|----------------------|-------------------------|------|----------------------------------------|
| `rpe_catalog/`        | `rpe-catalog`        | `com.rpe.catalog`       | 8080 | none (no SQS dependency)               |
| `rpe_client_manager/` | `rpe-client-manager` | `com.rpe.clientmanager` | 8082 | publishes to `rpe-client-manager-queue` |
| `rpe_card_processor/` | `rpe-card-processor` | `com.rpe.cardprocessor` | 8083 | consumes `rpe-client-manager-queue`     |

Package layout per service: `config`, `controller`, `service`, `repository`, `domain`, `client`
(Feign clients; not in catalog), `messaging` (SQS; not in catalog), `exception` (catalog so far). Empty folders hold a `.gitkeep`.

Which service calls which over HTTP is not defined yet — ask before wiring it (known so far:
rpe_card_processor will read products from rpe_catalog).

### rpe_catalog

CRUD for card products at `/api/v1/products` (`GET/PUT/DELETE /{id}`, `POST`). `ProductResponse` is the
contract rpe_card_processor will consume. `DELETE` is a soft delete (status → `CANCELADO`). Entities
extend `domain/AuditableEntity` (`created_at`/`updated_at` via Spring Data JPA auditing). Ids are UUIDs.
Product names are stored trimmed + upper-cased (`Product.normalizeName`, which rejects names empty after
trim) and are unique.

Errors: every intentional exception extends `exception/CustomException` and carries an `ErrorCode` (which holds
the HTTP status); business-rule violations extend `BusinessRuleException`. `GlobalExceptionHandler` renders
all errors — including Spring's own — as ProblemDetail with extra `code` and `timestamp` fields. To add an
error: add an `ErrorCode` constant and a `CustomException` subclass; no handler change needed.

## Requirements the services must cover

- Expose REST APIs and call other applications (Spring Cloud OpenFeign; external APIs stubbed by WireMock).
- Publish to / listen on SQS (Spring Cloud AWS 3.4.x, LocalStack locally).
- Persist to PostgreSQL (Spring Data JPA, `ddl-auto: validate`, schema managed by Flyway).
- Cache with Redis — **rpe_card_processor only** (`@EnableCaching`, `spring.cache.type=redis`, key prefix per service).

## Infrastructure (`docker-compose.yml`)

| Service    | Image                        | Host port | Notes                                                        |
|------------|------------------------------|-----------|--------------------------------------------------------------|
| postgres   | `postgres:17-alpine`         | 5432      | single database `rpe` (user/pass `app`/`app`) shared by all 3 |
| redis      | `redis:7-alpine`             | 6379      |                                                              |
| localstack | `localstack/localstack:4.14` | 4566      | SQS only; pinned because 2026.x tags may need an auth token  |
| wiremock   | `wiremock/wiremock:3.13.2`   | 8081      | stubs in `docker/wiremock/mappings`                          |
| kafka      | `apache/kafka:3.9.1`         | 9092      | commented out (precaution); `spring-kafka` commented in poms |

- `docker/localstack/init/ready.d/01-create-queues.sh` creates `rpe-client-manager-queue` + `-dlq`
  (redrive after 3 receives). The queue name comes from `CLIENT_MANAGER_QUEUE`.
- The three apps are under the compose profile `apps`; they share env via the `x-app-env` anchor and
  reach each other by service name (e.g. `http://rpe-catalog:8080`).

### Shared database + Flyway

All services use the same `rpe` database. Each has its own Flyway history table
(`flyway_history_catalog`, `flyway_history_client_manager`, `flyway_history_card_processor`) with
`baseline-on-migrate: true` / `baseline-version: 0`, so migrations from different services don't collide.
Migrations go in each service's `src/main/resources/db/migration`, starting at `V1__...`.
Because the tables share one schema, table names must not clash across services.

### Cross-cutting config conventions

- The three `application.yml` files are near-identical copies (only card_processor has the Redis/cache
  block; catalog also lacks `spring.cloud.aws`, `app.sqs` and `integrations`). A shared-config change must be applied to all three by hand.
- rpe_catalog only receives requests: it has no Feign, no WireMock test dependency and no Spring Cloud BOM.
  Don't add them back unless it starts calling another service.
- Feign base URLs go under `integrations.<name>.base-url` (currently `integrations.external-api.base-url`,
  env `EXTERNAL_API_BASE_URL`). WireMock is `localhost:8081` from the host but `wiremock:8080` inside compose.
- The queue name is injected from `app.sqs.client-manager-queue`. Adding a new queue means updating the
  LocalStack init script, the `x-app-env` anchor, and the `application.yml` of both sides.
- Lombok is available (annotation processor configured); `wiremock-spring-boot` is a test dependency for
  stubbing Feign calls in tests.

## Commands

```bash
docker compose up -d                       # infrastructure only
docker compose --profile apps up --build   # infrastructure + the 3 services
docker compose down -v                     # reset (drops the postgres volume)

cd rpe_<service> && ./mvnw spring-boot:run # run one service locally
cd rpe_<service> && ./mvnw test            # the contextLoads test needs the infra running
cd rpe_<service> && ./mvnw test -Dtest=ClassName#method   # single test
cd rpe_<service> && ./mvnw -B package -DskipTests         # build the jar (same as the Dockerfile)
```

The `@SpringBootTest` context-load tests connect to the real Postgres (plus Redis/LocalStack for the
services that use them), so run `docker compose up -d` first.

All configuration in `application.yml` reads environment variables with localhost defaults.

## Environment notes

- The developer works on Windows + WSL2. Maven is not installed globally — always use `./mvnw`.
