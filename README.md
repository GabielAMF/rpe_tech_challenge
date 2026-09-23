# RPE Tech Challenge

Three independent Spring Boot 3.5 / Java 21 microservices sharing one infrastructure stack.

## Services

| Service              | Folder                | Port | SQS                                      |
|----------------------|-----------------------|------|------------------------------------------|
| rpe-catalog          | `rpe_catalog/`        | 8080 | –                                        |
| rpe-client-manager   | `rpe_client_manager/` | 8082 | publishes to `rpe-client-manager-queue`  |
| rpe-card-processor   | `rpe_card_processor/` | 8083 | consumes `rpe-client-manager-queue`      |

Each service is a standalone Maven project with its own `pom.xml`, `mvnw` and `Dockerfile`.

## Infrastructure

| Concern                  | Technology                          | Local port |
|--------------------------|-------------------------------------|------------|
| Database (shared)        | PostgreSQL 17, database `rpe`       | 5432       |
| Cache (card processor)   | Redis 7                             | 6379       |
| Messaging                | AWS SQS via LocalStack              | 4566       |
| External API stubs       | WireMock                            | 8081       |
| Kafka (disabled for now) | Apache Kafka (KRaft)                | 9092       |

All services use the same database. Each one keeps its own Flyway history table
(`flyway_history_<service>`), so their migrations don't collide.

## Running

```bash
# infrastructure only, then run the services from the IDE or with maven
docker compose up -d
cd rpe_catalog && ./mvnw spring-boot:run

# infrastructure + all three services in containers
docker compose --profile apps up --build
```

LocalStack creates `rpe-client-manager-queue` (and its `-dlq`) on startup via
`docker/localstack/init/ready.d/01-create-queues.sh`. WireMock stubs live in `docker/wiremock/mappings`.

## Project decisions

Short notes on the choices made so far and why.

### UUIDs as ids, exposed in the URL (`/api/v1/products/{uuid}`)

Every entity uses a random UUID as its primary key, and that same value is the id in the REST paths.

- **Not guessable.** Sequential ids (`/products/1`, `/products/2`, ...) let anyone walk through every record and
  reveal how many exist. A random UUID can't be enumerated. This is an extra layer, not a replacement for
  authorization checks.
- **Unique across services.** A UUID can be generated anywhere without asking the database, so ids never
  collide between the three services sharing one database, and other services (e.g. rpe_card_processor
  storing a `productId`) reference it directly.
- **One id only.** We considered an internal `Long` primary key plus a separate public UUID, but it doubles
  indexes and lookups and risks leaking the internal id. A UUID can't be stored in a `Long` (128 vs 64 bits).
- **Cost accepted.** Longer URLs and a slightly larger index than `bigint`, which doesn't matter at this scale.
  If insert performance ever does, time-ordered UUIDs (v7) are a drop-in change.

### Soft delete

`DELETE` marks a product `CANCELADO` instead of removing the row. The audit history stays intact and other
services that reference the product don't end up pointing at a missing record.

### Normalized product names

Names are stored trimmed and upper-cased and must be unique, so `" gold "`, `"Gold"` and `"GOLD"` are the
same product and can't be created twice.

### Audit columns

Every table gets `created_at` / `updated_at`, filled automatically by Spring Data JPA auditing through a shared
base entity, so no service code has to remember to set them.

### Independent services, one database

Each service is its own Maven project (no parent pom) so it can be built, versioned and deployed alone. They
share one PostgreSQL database to keep the local setup simple, but each keeps its own Flyway history table so
their migrations stay independent.

### Redis only where it's needed

Only rpe_card_processor uses the Redis cache. rpe_catalog and
rpe_client_manager don't depend on Redis at all, so they start without it.

### Feign and WireMock only in services that call others

rpe_catalog only receives requests and never calls another service, so it has no Feign client and no WireMock
dependency, and it starts with just PostgreSQL. Services that do call others (e.g. rpe_card_processor reading
products from rpe_catalog) use Feign, and WireMock stubs those APIs in their tests and locally.
