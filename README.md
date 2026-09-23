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
| Cache                    | Redis 7                             | 6379       |
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
