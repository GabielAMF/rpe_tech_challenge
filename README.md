# RPE Tech Challenge

Three independent Spring Boot 3.5 / Java 21 microservices sharing one infrastructure stack.

## Services

| Service              | Folder                | Port | SQS                                      |
|----------------------|-----------------------|------|------------------------------------------|
| rpe-catalog          | `rpe_catalog/`        | 8080 | –                                        |
| rpe-client-manager   | `rpe_client_manager/` | 8082 | publishes to `rpe-client-manager-queue`  |
| rpe-card-processor   | `rpe_card_processor/` | 8083 | consumes `rpe-client-manager-queue`      |

Each service is a standalone Maven project with its own `pom.xml`, `mvnw` and `Dockerfile`.

> **Naming.** The challenge describes the services in Portuguese; the code uses English names:
> **Produto Service** (Catálogo) → `rpe-catalog` (*product*), **Portador Service** → `rpe-client-manager`
> (a portador is a *customer*), **Cartão Service** → `rpe-card-processor` (*card*). Domain values stay in Portuguese
> as the challenge defines them (`ATIVO`, `BLOQUEADO`, `CANCELADO`).

### API documentation (OpenAPI / Swagger)

| Service            | Swagger UI                              | OpenAPI JSON                        |
|--------------------|-----------------------------------------|-------------------------------------|
| rpe-catalog        | http://localhost:8080/swagger-ui.html   | http://localhost:8080/v3/api-docs   |
| rpe-client-manager | http://localhost:8082/swagger-ui.html   | http://localhost:8082/v3/api-docs   |
| rpe-card-processor | http://localhost:8083/swagger-ui.html   | http://localhost:8083/v3/api-docs   |

The docs are public. In rpe-client-manager, call `POST /api/v1/auth/login` from Swagger UI, then paste the
`accessToken` into **Authorize** to try the other endpoints.

## Infrastructure

| Concern                  | Technology                          | Local port |
|--------------------------|-------------------------------------|------------|
| Database (shared)        | PostgreSQL 17, database `rpe`       | 5432       |
| Cache (card processor)   | Redis 7                             | 6379       |
| Messaging                | AWS SQS via LocalStack              | 4566       |
| Kafka (disabled for now) | Apache Kafka (KRaft)                | 9092       |

All services use the same database. Each one keeps its own Flyway history table
(`flyway_history_<service>`), so their migrations don't collide.

## Running

```bash
# the whole environment (infrastructure + the three services) with one command
docker compose up --build            # add -d to run in the background, --wait to block until all are healthy

# infrastructure only, then run the services from the IDE or with maven
docker compose up -d postgres redis localstack
cd rpe_catalog && ./mvnw spring-boot:run

# reset everything (drops the database volume)
docker compose down -v
```

LocalStack creates `rpe-client-manager-queue` (and its `-dlq`) on startup via
`docker/localstack/init/ready.d/01-create-queues.sh`. The services call each other for real (no stubs at
runtime); WireMock is only used inside the tests.

[`docs/TEST_PLAN.md`](docs/TEST_PLAN.md) has a step-by-step manual test plan (curls) for the services running
together, including the failure scenarios.

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

### Seeded products

The catalog starts with BLACK CARD, PREMIUM, GOLD and PLATINUM (Flyway `V2__seed_card_products.sql`) under fixed
ids, so other services can reference them; GOLD is rpe-card-processor's default product. A name that already
exists is skipped, so on a database that had a GOLD before the seed, that row keeps its own id.

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

A final SOLID review of the three services (after all features) applied the same rules everywhere:

- **Dependencies point inward.** The AES ciphers moved from `service` to `repository/converter`, next to the only
  classes that use them, so entities → converters no longer leads back into the service layer. `TokenService` is an
  interface; its JWT implementation lives in `config` and owns the claim names `SecurityConfig` reads.
- **Open for extension.** `ProductSelectionPolicy` is an interface: the real credit analysis will be a new
  implementation replacing the placeholder `CreditInfoProductSelectionPolicy`, without touching the service.
- **One place per rule.** `CustomerName` joins `Cpf`/`Username`/`ProductName` as a validating value object, and
  `CardProductionCommand` validates itself instead of the SQS listener doing it. Every response goes through a
  `...Mapper` (`UserMapper` added).
- **Accepted trade-offs.** Entities keep `@Convert(EncryptedStringConverter.class)`: like the other JPA annotations,
  it's mapping metadata, and the alternative (XML mapping) costs more than it gives. Services read typed
  `config.*Properties` records (settings, not infrastructure). `SqsOutboxEventSender` routes its single event type
  with an `if`; a routing table can come with a second event type.

### Tests isolated from the running applications

The integration tests use the real infrastructure but never the applications' data: database `rpe_test` (created by
`docker/postgres/init`), their own SQS queues and their own Redis key prefix. So `./mvnw test` can run while
`docker compose up` is running, and the tests never consume, cache or create anything the apps see. On a volume
created before this change, create the database once: `docker exec rpe-postgres createdb -U app rpe_test`.

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

### Card production (client-manager → SQS → card-processor)

Creating a customer publishes a `CARD_PRODUCTION_REQUESTED` message to `rpe-client-manager-queue` with the
customer id, name, CPF and the request's `credit_info`, so rpe-card-processor has what it needs without calling
back. `credit_info` is only forwarded: it is never stored with the customer, only (encrypted) in the outbox until sent. `GET /customers/{id}` returns the customer plus its card and
product from rpe-card-processor (`GET /api/v1/customers/{customerId}/card`). If that service is down the customer
is still returned, with `"card": null` and `"cardInfoAvailable": false`; before the card is produced, `"card"` is
null and `"cardInfoAvailable"` is true.

### Transactional outbox and idempotency

Creating a customer never talks to SQS. The card production request is saved in the `outbox_event` table **in the
same transaction** as the customer, so both are committed or neither is: no customer without a request, no request
for a customer that doesn't exist, and no 503 when SQS is down. `OutboxRelay` (every second) locks due events with
`FOR UPDATE SKIP LOCKED` (safe with several instances), sends them and marks them SENT.

- **At-least-once delivery.** If the relay dies after SQS accepted an event but before marking it SENT, the event
  is sent again, with the same `eventId`. SQS standard queues can also deliver twice. Duplicates are expected.
- **Idempotent consumer.** rpe-card-processor creates at most one card per event (`uk_card_source_event`) and per
  customer (`uk_card_customer`); a repeat returns the existing card and is acknowledged. If two copies are
  processed at the same moment, the loser hits the unique constraint, is retried by SQS and then finds the card.
- **Idempotent API.** A retried `POST /customers` can't create a second customer: the CPF is unique, so the
  retry gets 409. No `Idempotency-Key` header is needed. `DELETE` is idempotent (204 again); `activate` on an
  active customer is 422.
- **Retries.** A failed send is retried with exponential backoff (2s doubling up to 5 min). After 12 attempts
  (about 23 minutes) the event is marked **FAILED** and logged at ERROR; it keeps its payload so it can be resent
  once the cause is fixed:
  `UPDATE outbox_event SET status = 'PENDING', attempts = 0, next_attempt_at = now() WHERE id = '<id>';`
  Giving up (instead of retrying forever) makes a stuck event visible instead of hiding it in endless retries.
- **Personal data.** The payload carries name, CPF and credit info, so it is AES-256-GCM encrypted in the table
  (`OUTBOX_ENCRYPTION_KEY`, always set outside local development) and **cleared as soon as the event is SENT**.
  SENT rows (without payload) are kept 7 days for debugging, then purged by a daily job.

### Card processor: product choice, card data and encryption

- **Consuming**: rpe-card-processor listens on `rpe-client-manager-queue`. A failed message is retried with
  backoff and ends up in the DLQ (see "SQS retry and dead-letter queue" below). Processing is idempotent: the same
  message, or a second one for the same customer, never creates a second card.
- **Product (placeholder rule)**: there is no credit analysis yet, so `credit_info` is read as a catalog product
  id; if it isn't one, or that product isn't ATIVO, the default product (GOLD) is used. Catalog responses are
  cached in Redis for 10 minutes.
- **Card data**: the operator (VISA, MASTERCARD, ELO) is picked at random and the 16-digit number is generated with
  that operator's prefix and a valid Luhn check digit, so both always agree. Expiry is month/year, 5 years ahead.
- **Sensitive data**: number, expiry and CVV are encrypted in the database (AES-256-GCM, key in
  `CARD_ENCRYPTION_KEY`, 32 random bytes in base64, always set outside local development). Only the masked number
  (`**** **** **** 1234`) is ever exposed, and none of it is logged.
- **Reading a card**: `GET /api/v1/customers/{customerId}/card` (404 until the card exists) returns the card
  status, masked number and product. Both when creating and when reading a card, the product comes from rpe_catalog
  through the Redis cache, so a renamed or cancelled product shows up (up to 10 minutes late, the cache TTL; fine
  because products change rarely and not without notice). The card also stores a snapshot of the product (id,
  name, description) from issue time: if the catalog can't answer, the card is still returned with that snapshot
  and `product.status: null`.
- **No authentication**: the challenge only requires authentication on rpe-client-manager. The card API is
  internal (only rpe-client-manager calls it) and open; exposing it would need service-to-service authentication
  (e.g. a client-credentials token or mTLS).

### SQS retry and dead-letter queue (rpe-card-processor)

A card production message that fails (e.g. rpe-catalog is down) is not lost and not retried in a tight loop:

1. **Retry with exponential backoff.** When the listener throws, the message isn't deleted. Spring Cloud AWS's
   `ExponentialBackoffErrorHandler` (`SqsConfig`) sets the message's visibility timeout from its receive count, so
   SQS redelivers it **5s**, then **20s** after a failure (`app.sqs.retry.*`). Without it, every retry would wait
   the queue's fixed 30s visibility timeout (the time allowed to process a message).
2. **Dead-letter queue.** The queue's redrive policy moves a message to `rpe-client-manager-queue-dlq` after
   **3 failed receives**. Dead messages are kept 14 days (the SQS maximum). A message that can never succeed
   (e.g. missing fields) takes the same path, so nothing is dropped silently.
3. **Alerting.** `DeadLetterQueueMonitor` checks the DLQ size every minute and logs at **ERROR** while it isn't
   empty. It doesn't consume the DLQ (that would delete the messages). In AWS this would be a CloudWatch alarm on
   the DLQ's `ApproximateNumberOfMessagesVisible`.
4. **Redrive.** Once the cause is fixed, move the messages back to the main queue; processing is idempotent, so a
   message that did get processed meanwhile does no harm:

```bash
# inspect (peek without removing)
docker exec rpe-localstack awslocal sqs receive-message --visibility-timeout 0 --max-number-of-messages 10 \
  --queue-url http://localhost:4566/000000000000/rpe-client-manager-queue-dlq
# move everything back to rpe-client-manager-queue
docker exec rpe-localstack awslocal sqs start-message-move-task \
  --source-arn arn:aws:sqs:us-east-1:000000000000:rpe-client-manager-queue-dlq
```

Tested end to end in `CardProductionRetryIntegrationTest` (3 attempts with growing gaps, then the DLQ).

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
products from rpe_catalog) use Feign. WireMock stubs the called APIs **only in tests** (in-process, per test
class), including their failure modes; at runtime, locally and in compose, the services call each other for real,
so there is no WireMock container.
