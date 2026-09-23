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

### Authentication (rpe-client-manager)

Every rpe-client-manager endpoint except login needs a JWT. On startup the service creates an ADMIN user from
`AUTH_BOOTSTRAP_USERNAME` / `AUTH_BOOTSTRAP_PASSWORD` (locally `admin` / `admin12345`):

```bash
TOKEN=$(curl -s localhost:8082/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin12345"}' | jq -r .accessToken)
curl -H "Authorization: Bearer $TOKEN" localhost:8082/api/v1/...
```

Admins can create more users with `POST /api/v1/auth/users` (`{"username", "password", "role": "ADMIN|USER"}`).
Outside local development, always set `JWT_SECRET` (at least 32 characters) and the bootstrap password.

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
services that reference the product don't end up pointing at a missing record. A cancelled product is brought
back with `POST /api/v1/products/{id}/activate`; status never changes through `PUT`, so each status change is
an explicit action.

### Normalized product names

Names are stored trimmed and upper-cased and must be unique, so `" gold "`, `"Gold"` and `"GOLD"` are the
same product and can't be created twice.

### Audit columns

Every table gets `created_at` / `updated_at`, filled automatically by Spring Data JPA auditing through a shared
base entity, so no service code has to remember to set them.

### Code structure: layered, following SOLID

We compared three options for the catalog: minimal fixes, a layered clean-up, and hexagonal architecture
(one class per use case, ports and adapters). Hexagonal was overkill for five operations, and minimal fixes
left too many SOLID issues, so we chose the layered clean-up:

- **S** — each class has one job: `ProductName` (name rules), `ProductNamePolicy` (uniqueness rule),
  `ProductMapper` (entity → response), `ProductServiceImpl` (use cases).
- **O** — new errors only need an `ErrorCode` and a `CustomException` subclass; the handler doesn't change.
- **I** — consciously relaxed for repositories to follow the `JpaRepository` convention (see below).
- **D** — the controller depends on the `ProductService` interface, and the service works with domain types,
  never the web DTOs.

### Repositories: `JpaRepository` by convention

Repositories extend `JpaRepository`, the usual Spring Data convention, even though it exposes hard-delete
methods (`deleteById`, `deleteAll`, ...) that the soft-delete rule forbids. We considered extending the bare
`Repository` interface and declaring only the methods used, which makes a hard delete impossible to compile,
but chose familiarity: the rule is enforced by the service layer and code review, and documented on
`ProductRepository`.

### Authentication: JWT issued by the service, users in the database

rpe-client-manager issues and validates its own HS256 tokens (Spring Security resource server), with users and
BCrypt password hashes in its database. An external identity provider (e.g. Keycloak) would be more
production-like but adds a whole extra service; a single shared secret is enough while only this service
validates tokens. Tokens are stateless and last 1 hour (`JWT_EXPIRATION`), so there is no logout or revocation.

### Customers: CPF, status and personal data

- **CPF** is stored as 11 letters/digits without formatting. Letters are allowed because the CPF is expected to
  become alphanumeric, so check digits aren't validated until those rules exist. It is unique and can't change.
- **Status**: `DELETE` cancels, `POST /{id}/activate` reactivates, and `PUT` can only block. A cancelled customer
  can come back (its CPF can never be reused), so creating one with a cancelled customer's CPF points to activate.
- **Minimum age** is implemented but disabled (`CUSTOMER_MINIMUM_AGE=0`): it was considered, but the real value
  depends on the product and regulation and isn't confirmed yet.
- **Personal data** never reaches the logs: only the customer id and a masked CPF (`***.***.***-09`).

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
