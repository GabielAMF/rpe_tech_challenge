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

Package layout per service: `config`, `controller`, `service`, `repository`, `domain`, `exception`, `client`
(Feign; not in catalog), `messaging` (SQS; not in catalog). Empty folders hold a `.gitkeep`.

How the services talk (ask before adding any other call):
- rpe_client_manager → SQS `rpe-client-manager-queue` → rpe_card_processor (card production request).
- rpe_client_manager → HTTP → rpe_card_processor `GET /api/v1/customers/{customerId}/card` (card + product for
  `GET /customers/{id}`).
- rpe_card_processor → HTTP → rpe_catalog `GET /api/v1/products/{id}` (cached in Redis).
- Only rpe_client_manager requires a JWT (the challenge only asks for auth there). rpe_card_processor's API is open,
  internal-only — a scope choice, not a to-do.

### rpe_catalog

CRUD for card products at `/api/v1/products` (`GET/PUT/DELETE /{id}`, `POST`, `POST /{id}/activate`).
`ProductResponse` is the contract rpe_card_processor will consume. Status only changes through `DELETE`
(soft delete → `CANCELADO`, idempotent) and `activate` (→ `ATIVO`; 422 if already active); `PUT` never touches it. Entities
extend `domain/AuditableEntity` (`created_at`/`updated_at` via Spring Data JPA auditing). Ids are UUIDs.
Product names are stored trimmed + upper-cased and are unique. `V2__seed_card_products.sql` seeds BLACK CARD,
PREMIUM, GOLD and PLATINUM with fixed ids (`ON CONFLICT DO NOTHING` on the name); GOLD
`3f2b6c1e-8d4a-4f7b-9c2e-1a5d6e7f8a9b` is rpe_card_processor's default product.

Layering (keep dependencies pointing inward):
- `controller` owns the HTTP contract: request/response DTOs, `ProductMapper` (entity → `ProductResponse`),
  and building `ProductName` from requests. Nothing outside `controller` may import `controller.*`.
- `service`: `ProductService` interface + `ProductServiceImpl`, working only with domain types. Business rules
  that need the database live in their own components (`ProductNamePolicy`: uniqueness / cancelled-name rule).
- `domain`: `Product` plus value objects (`ProductName` normalizes and rejects empty names). State
  transitions (`cancel`, `activate`) and their rules live on the entity.
- `repository`: `ProductRepository extends JpaRepository` (a conscious convention choice, see README).
  Never call its delete methods — products are soft-deleted; only the service changes status.

Errors: every intentional exception extends `exception/CustomException` and carries an `ErrorCode` (which holds
the HTTP status); business-rule violations extend `BusinessRuleException`. `GlobalExceptionHandler` renders
all errors — including Spring's own — as ProblemDetail with extra `code` and `timestamp` fields. To add an
error: add an `ErrorCode` constant and a `CustomException` subclass; no handler change needed.

### rpe_client_manager

Same layering and error format as rpe_catalog (base classes copied, not shared — services stay independent).
Security (`config/SecurityConfig`): stateless JWT, HS256 via `JwtEncoder`/`JwtDecoder`, issued by
`POST /api/v1/auth/login` (public); everything else needs a bearer token. Users live in `app_user` (BCrypt);
an ADMIN is created on startup from `app.security.bootstrap-user`. `AuthService` depends on the `TokenService`
interface; `config/JwtTokenService` implements it and owns the claim names (`ROLES_CLAIM`, read by `SecurityConfig`).
`AuthController` builds responses with `UserMapper`. Roles come from the `roles` claim and are
enforced with URL rules in `SecurityConfig`, **not** `@PreAuthorize` (an `AccessDeniedException` thrown in a
controller would hit `GlobalExceptionHandler`'s catch-all and become a 500). 401/403 are written by
`SecurityProblemHandler` in the same ProblemDetail shape. Never log passwords or tokens.

Customers at `/api/v1/customers` (any authenticated user): `GET/PUT/DELETE /{id}`, `POST`, `POST /{id}/activate`.
- `CustomerName` value object (trimmed, non-empty, `toString()` hidden) is built in the controller like `Cpf`;
  `CustomerService`/`Customer` take it, never a raw String.
- `Cpf` value object: formatting stripped, upper-cased, exactly 11 letters/digits (alphanumeric CPF is expected;
  check digits deliberately not validated yet). Unique and immutable (`updatable = false`, not in the PUT DTO).
- Status: `DELETE` → CANCELADO (idempotent); `activate` → ATIVO from BLOQUEADO/CANCELADO (422 if already
  ATIVO); `PUT` may only set `status` to BLOQUEADO (or repeat the current one). CANCELADO is not final because
  a CPF can never be reused: creating with a cancelled customer's CPF → 409 `CANCELLED_CUSTOMER_EXISTS`.
- `BirthDatePolicy`: birth date must be in the past; `app.customer.minimum-age` exists but defaults to 0
  (disabled) until the real rule is confirmed.
- Personal data: logs only carry the customer id and `Cpf.masked()` (`Cpf.toString()` is masked too). Never log
  names, birth dates, full CPFs or raw database constraint messages (they contain the duplicated value).
- Card production uses a **transactional outbox**: `create` calls `CardProductionPublisher` (impl
  `service/OutboxCardProductionPublisher`, `Propagation.MANDATORY`), which saves the `CardProductionRequested` JSON
  as an `OutboxEvent` in the customer's transaction — no SQS call in the request, no 503. `service/OutboxRelay`
  (`@Scheduled`, `app.outbox.poll-interval`) locks due PENDING rows (`FOR UPDATE SKIP LOCKED`, one transaction per
  batch, stops at the first failure) and sends them via `OutboxEventSender` → `messaging/SqsOutboxEventSender`
  (plain JSON, no Java type header — see `SqsConfig`; `app.sqs.send-timeout` per send). Retry rules live in
  `domain/OutboxRetryPolicy` + `OutboxEvent.recordFailure` (backoff, then FAILED after `max-attempts`; FAILED keeps
  the payload for a manual resend, SQL in README). Payload is AES-GCM encrypted (`repository/converter/PayloadCipher`
  + `EncryptedStringConverter`, `app.outbox.encryption-key`) and nulled on SENT; `OutboxPurger`
  deletes SENT rows after `app.outbox.retention`. Delivery is at least once with a stable `eventId`; the consumer
  de-duplicates. `CardProductionRequested` is the message contract; its `toString()` hides personal data.
- `GET /customers/{id}` → `CustomerService.getDetails` (not transactional: no DB transaction during HTTP) uses
  `CardInfoGateway` → Feign `CardProcessorClient` (`integrations.card-processor.base-url`,
  `localhost:8083` / `rpe-card-processor:8083` in compose; 1s connect / 2s read timeout). The gateway never throws: 404 → no card yet, any other failure → unavailable.

### rpe_card_processor

Same layering/exception base (copied). `messaging/CardProductionListener` (`@SqsListener` on
`app.sqs.client-manager-queue`) consumes rpe_client_manager's `CARD_PRODUCTION_REQUESTED` JSON
(`CardProductionRequestedMessage` = the contract) → `CardProductionService.produce`. Throwing leaves the message
un-acked → SQS retries → DLQ after 3 receives. Idempotent: one card per customer (`uk_card_customer`) and per
event (`uk_card_source_event`); a repeat returns the existing card.
- `ProductSelectionPolicy` is an interface; the current implementation `CreditInfoProductSelectionPolicy` is an explicit
  **placeholder**: `creditInfo` parsed as a catalog product id (used if it exists and is ATIVO), else
  `app.card.default-product-id` (seeded GOLD). The real rule = a new implementation, no change to the service.
- `CardProductionCommand` validates itself (eventId, customerId, holder name) → `InvalidCardProductionRequestException`;
  the listener only maps the message and delegates.
- Retry/DLQ: `config/SqsConfig` registers an `ExponentialBackoffErrorHandler` bean (picked up by Spring Cloud AWS's
  default listener factory; `app.sqs.retry.*`: 5s ×4, cap 5m) that sets the failed message's visibility from its
  receive count. Queue `VisibilityTimeout=30`, redrive after 3 receives, DLQ retention 14 days (LocalStack init
  script). `messaging/DeadLetterQueueMonitor` (`@Scheduled`, `app.sqs.dlq-monitor.*`, disabled in the test yml)
  logs ERROR while the DLQ (`app.sqs.client-manager-dlq`) isn't empty — it never consumes it. Redrive with
  `start-message-move-task` (README). `CardProductionRetryIntegrationTest` creates its own queue + DLQ.
- `CachedCatalogGateway` → Feign `CatalogClient` (`integrations.catalog.base-url`), `@Cacheable` in Redis cache
  `catalog-products` as JSON (`CacheConfig`); only found products are cached (Optional + `unless`).
- `CardDataGenerator`: random operator (VISA/MASTERCARD/ELO), number = operator prefix + random digits + Luhn,
  expiry `YearMonth` + `app.card.validity-years`, 3-digit CVV, from a `SecureRandom` bean.
- Number, expiry and CVV are AES-256-GCM encrypted at rest by JPA converters (`repository/converter`, Spring beans
  using `repository/converter/CardCipher`, key `app.card.encryption-key` = base64 of 32 bytes). Only `maskedNumber` may leave the
  service; never log card data, the holder name, CPF or credit info (`toString()`s of messages/commands hide them).
- `GET /api/v1/customers/{customerId}/card` (`CustomerCardController` → `CardQueryService` → `CardMapper`/`CardResponse`,
  the contract mirrored by client_manager's `CardProcessorCardResponse`): 404 `CARD_NOT_FOUND` until produced.
  The product (id/name/description/status) is read through `CatalogGateway` (cached); if the catalog fails or no
  longer knows it, the card's snapshot (`CardProduct`) is used with a null status — never hidden by an outage.
  Errors go through `controller/GlobalExceptionHandler` (copied from client_manager).
- Spring-context tests listen on `rpe-card-processor-test-queue`, the flow/retry tests on per-run queues, and the
  test cache prefix is `rpe-card-processor-test::`, so tests never touch what a running container uses.

## Conventions to follow in new code

These were agreed with the user while building rpe_catalog and rpe_client_manager; keep new code consistent.
- Ids are UUIDs (`GenerationType.UUID`, `uuid` column), also in URLs.
- Action endpoints use POST (`POST /{id}/activate`), never PATCH. `DELETE` is a soft delete to CANCELADO and is
  idempotent (204 again); status changes only through dedicated endpoints/methods, not a generic PUT field.
- Normalized input becomes a value object record that validates in its constructor (`ProductName`, `Username`,
  `Cpf`, `CustomerName`, `CardProductionCommand`); rules that may be replaced sit behind an interface
  (`ProductSelectionPolicy`, `TokenService`); crypto lives in `repository/converter` next to its converters; rules that need the database live in a `...Policy` component; services are interface + `...Impl` and
  work with domain types only; a `...Mapper` in `controller` builds response DTOs.
- Errors: add an `ErrorCode` + `CustomException`/`BusinessRuleException` subclass. Conflicts that point to a
  cancelled record return its id (`productId`/`customerId`) and say how to reactivate it. 422 for business-rule
  violations on a valid request (e.g. already active).
- Personal/sensitive data (names, CPF, birth date, credit info, card number/expiry/CVV) never reaches logs or error
  messages; records carrying it override `toString()`.
- Tests: unit tests per domain/policy/service class; `@WebMvcTest` for controllers (in client_manager import
  `SecurityConfig`, `SecurityProblemHandler`, `ClockConfig` and use `jwt()`); one `@SpringBootTest` end-to-end
  test per flow against the real infrastructure; entity fixtures set `id`/timestamps with `ReflectionTestUtils`.
- Every design decision with a trade-off gets a short entry in README "Project decisions"; CLAUDE.md is kept in
  sync when behaviour or structure changes.

## Requirements the services must cover

- Expose REST APIs and call other applications (Spring Cloud OpenFeign; called APIs stubbed by WireMock in tests).
- Publish to / listen on SQS (Spring Cloud AWS 3.4.x, LocalStack locally).
- Persist to PostgreSQL (Spring Data JPA, `ddl-auto: validate`, schema managed by Flyway).
- Cache with Redis — **rpe_card_processor only** (`@EnableCaching`, `spring.cache.type=redis`, key prefix per service).

## Infrastructure (`docker-compose.yml`)

| Service    | Image                        | Host port | Notes                                                        |
|------------|------------------------------|-----------|--------------------------------------------------------------|
| postgres   | `postgres:17-alpine`         | 5432      | single database `rpe` (user/pass `app`/`app`) shared by all 3 |
| redis      | `redis:7-alpine`             | 6379      |                                                              |
| localstack | `localstack/localstack:4.14` | 4566      | SQS only; pinned because 2026.x tags may need an auth token  |
| kafka      | `apache/kafka:3.9.1`         | 9092      | commented out (precaution); `spring-kafka` commented in poms |

- `docker/localstack/init/ready.d/01-create-queues.sh` creates `rpe-client-manager-queue` + `-dlq`
  (redrive after 3 receives). The queue name comes from `CLIENT_MANAGER_QUEUE`.
- The three apps start with the infrastructure (no profile) and have actuator healthchecks (`x-app-healthcheck`,
  busybox `wget`); card-processor waits for the catalog to be healthy. They share env via the `x-app-env` anchor and
  reach each other by service name (e.g. `http://rpe-catalog:8080`).

### Shared database + Flyway

All services use the same `rpe` database. Each has its own Flyway history table
(`flyway_history_catalog`, `flyway_history_client_manager`, `flyway_history_card_processor`) with
`baseline-on-migrate: true` / `baseline-version: 0`, so migrations from different services don't collide.
Migrations go in each service's `src/main/resources/db/migration`, starting at `V1__...`.
Because the tables share one schema, table names must not clash across services.

### Cross-cutting config conventions

- The three `application.yml` files share the datasource/JPA/Flyway/actuator blocks, copied by hand: a
  shared-config change must be applied to all three. Differences: client_manager adds `app.security`,
  `app.customer`, SQS and Feign (`card-processor`); card_processor adds Redis cache, `app.card`, SQS and Feign
  (`catalog`); catalog has none of those.
- rpe_catalog only receives requests: it has no Feign, no WireMock test dependency and no Spring Cloud BOM.
  Don't add them back unless it starts calling another service.
- Feign base URLs go under `integrations.<name>.base-url` (`card-processor` → `CARD_PROCESSOR_BASE_URL`,
  `catalog` → `CATALOG_BASE_URL`), timeouts under `spring.cloud.openfeign.client.config.<name>`. The defaults point
  at the real services; there is no WireMock container any more.
- WireMock is **test-only**: `@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "integrations.<name>.base-url"))`
  starts an in-process server per test class (see `CardProcessorCardInfoGatewayTest`, `CustomerFlowIntegrationTest`,
  `CachedCatalogGatewayTest`). The services working together are validated by hand against compose (Status step 2).
- The queue name is injected from `app.sqs.client-manager-queue`. Adding a new queue means updating the
  LocalStack init script, the `x-app-env` anchor, and the `application.yml` of both sides.
- Lombok is available (annotation processor configured); `wiremock-spring-boot` is a test dependency for
  stubbing Feign calls in tests.
- OpenAPI: `springdoc-openapi-starter-webmvc-ui` (`springdoc.version` property, 2.8.x = Boot 3.5 line) in all three,
  `config/OpenApiConfig` per service; Swagger UI at `/swagger-ui.html`. client_manager permits `/v3/api-docs/**`
  and `/swagger-ui/**` in `SecurityConfig`, declares a global `bearer-jwt` scheme, and login opts out with
  `@SecurityRequirements`. `OpenApiDocsIntegrationTest` in each service.
- The challenge text is in Portuguese: Produto Service = rpe_catalog, Portador Service = rpe_client_manager,
  Cartão Service = rpe_card_processor (README "Naming").

## Commands

```bash
docker compose up --build                         # everything: infrastructure + the 3 services
docker compose up -d postgres redis localstack    # infrastructure only (to run services with mvnw/IDE)
docker compose down -v                            # reset (drops the postgres volume)

cd rpe_<service> && ./mvnw spring-boot:run # run one service locally
cd rpe_<service> && ./mvnw test            # the contextLoads test needs the infra running
cd rpe_<service> && ./mvnw test -Dtest=ClassName#method   # single test
cd rpe_<service> && ./mvnw -B package -DskipTests         # build the jar (same as the Dockerfile)
```

The `@SpringBootTest` tests connect to the real Postgres (plus Redis/LocalStack for the services that use them),
so start the infrastructure first. They are **isolated from the compose apps**, so they can run while the apps are
up: each service's `src/test/resources/config/application.yml` points to database `rpe_test` (created by
`docker/postgres/init` on a new volume; on an old one: `docker exec rpe-postgres createdb -U app rpe_test`), test
queues (`rpe-client-manager-test-queue`, `rpe-card-processor-test-queue`, per-run queues) and a test cache prefix.
Stop the app containers (`docker compose stop rpe-catalog rpe-client-manager rpe-card-processor`) only before
running a service with `mvnw spring-boot:run` (same ports, same queue).

All configuration in `application.yml` reads environment variables with localhost defaults.

## Git workflow

- Work happens on branches cut from `dev`: `feature/...`, `chore/...`, `refactor/...`. The user opens a PR into
  `dev`, and releases go `dev` → `master`. Never commit on `dev`/`master` directly.
- Commit only when the user asks. Pushing is done by the user: this shell has no SSH agent, so `git push` and
  `git fetch` fail with `Permission denied (publickey)` — ask the user to run them (`! git push ...`).
  The user usually pulls after merging, so local `dev` normally already has the merge; check `git log dev`.
- `TODO.md` (repo root) is the user's local to-do list and is gitignored. A to-do that must be committed goes in
  the code as `TODO(topic): ...` plus a README note.

## Environment notes

- The developer works on Windows + WSL2 with Docker Desktop. Maven is not installed globally — always use `./mvnw`.
- Docker Desktop/WSL bind-mount glitch: after `stop`/`restart`, localstack may fail with
  "error mounting ... no such file or directory". Fix: `docker compose up -d --force-recreate <service>`.
- LocalStack keeps nothing: after a restart the init script recreates the queues, but messages are gone.
  rpe_card_processor won't start (its listener fails) if LocalStack is down.
- Local login for rpe_client_manager: `admin` / `admin12345` (bootstrap user). `jq` is not installed in WSL
  (the README uses it; the user is fine with that) — use `python3 -c` to parse JSON in commands.
- Before `docker compose down -v`, the local database had a GOLD product with a random id, so card_processor's
  default product id didn't match. A clean `down -v` + restart seeds GOLD with the fixed id.

## Status and next steps (as of 2026-09-24)

Kept committed on purpose, to track what's missing. Update it when a branch is merged.

Done and merged into `dev` (in order): catalog CRUD → exception consistency → remove Feign from catalog →
status endpoints → SOLID refactor → client_manager JWT auth → customer CRUD → card production (SQS publish +
aggregated GET) → catalog product seed → card_processor card production → card_processor card API (WireMock
test-only) → client_manager transactional outbox + idempotency definitions → OpenAPI/Swagger → single-command
compose → SQS retry/backoff + DLQ monitor. The full challenge requirements were checked on 2026-09-24; every gap
found is closed.

In progress: `refactor/solid-review` — SOLID review fixes (see README "Code structure") + tests isolated from the
compose apps (`rpe_test` database, test queues, test cache prefix). All requirement gaps closed and merged.

Next: the user's integrated test from scratch (`docker compose down -v && docker compose up --build`), including
the unhappy paths: card processor stopped → `cardInfoAvailable: false`; catalog stopped → snapshot product with
null status; LocalStack stopped → customer still created (201), event PENDING until LocalStack is back; a message
failing 3 times → DLQ + monitor ERROR → redrive.
