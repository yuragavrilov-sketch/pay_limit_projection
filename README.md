# pay-limit-projection

`pay-limit-projection` is the asynchronous reservation audit projection service
for `PAY_ALL`.

The service consumes reservation lifecycle events from Kafka topic
`pay-limit-engine-reservation-events` and writes durable audit/reporting state to
Postgres schema `pay_limit_projection`. It is not part of the online limit
decision path: `pay-limit-engine` does not read this projection when evaluating,
holding, confirming, or rolling back operations.

The first increment exposes actuator endpoints only. Business read APIs and
projection tables are added in later increments.

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring Cloud 2025.1.1
- Spring MVC
- Spring JDBC
- Spring Kafka
- Flyway
- PostgreSQL
- Actuator and Prometheus metrics
- Config Server for non-secret configuration
- Vault for secrets

## Configuration

| Property | Environment variable | Default | Purpose |
| --- | --- | --- | --- |
| `server.port` | `SERVER_PORT` | `8088` | HTTP and actuator port. |
| `pay.environment` | `PAY_ENVIRONMENT` | `local` | Environment selector used for Config Server label and Vault paths. |
| `spring.cloud.config.enabled` | `CONFIG_SERVER_ENABLED` | `false` | Enables Config Server import outside local runs. |
| `spring.cloud.config.label` | `CONFIG_SERVER_LABEL` | `${pay.environment}` | Config Server git branch label. |
| `spring.cloud.vault.enabled` | `VAULT_ENABLED` | `false` | Enables Vault import outside local runs. |
| `spring.cloud.vault.kv.backend` | `VAULT_KV_BACKEND` | `pay` | Vault KV backend. |
| `spring.cloud.vault.kv.application-name` | `VAULT_KV_CONTEXTS` | empty | Comma-separated Vault contexts to read. |
| `spring.datasource.url` | `PAY_LIMIT_PROJECTION_DB_URL` | `jdbc:postgresql://localhost:5432/pay_admin?currentSchema=pay_limit_projection` | Projection database URL. |
| `spring.datasource.username` | `PAY_LIMIT_PROJECTION_DB_USERNAME` | `pay_admin` | Projection database user. |
| `spring.datasource.password` | `PAY_LIMIT_PROJECTION_DB_PASSWORD` | empty | Local fallback database password. |
| `spring.flyway.enabled` | `FLYWAY_ENABLED` | `false` | Enables Flyway migrations when a local/compose DB is available. |
| `pay-limit-projection.kafka.enabled` | `KAFKA_ENABLED` | `false` | Enables reservation event consumption. |
| `pay-limit-projection.kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers. |
| `pay-limit-projection.kafka.topic` | `KAFKA_TOPIC` | `pay-limit-engine-reservation-events` | Reservation lifecycle event topic. |
| `pay-limit-projection.kafka.group-id` | `KAFKA_GROUP_ID` | `pay-limit-projection` | Kafka consumer group. |

The database password is a secret and belongs in Vault at:

```text
pay/{environment}/pay-limit-projection-db-password
```

The Vault secret should store the target Spring property key, for example:

```properties
spring.datasource.password=<secret>
```

## Run Locally

Local startup does not require Config Server or Vault by default.

```powershell
mvn spring-boot:run
```

Useful local overrides:

```powershell
$env:PAY_LIMIT_PROJECTION_DB_URL="jdbc:postgresql://localhost:5432/pay_admin?currentSchema=pay_limit_projection"
$env:PAY_LIMIT_PROJECTION_DB_USERNAME="pay_admin"
$env:PAY_LIMIT_PROJECTION_DB_PASSWORD=""
$env:KAFKA_ENABLED="false"
mvn spring-boot:run
```

## Test

Run the startup test:

```powershell
mvn "-Dtest=PayLimitProjectionApplicationTests" test
```

Run the full test suite:

```powershell
mvn test
```

## Build Image

```powershell
docker build -t pay-limit-projection .
```
