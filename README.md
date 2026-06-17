# pay-limit-projection

`pay-limit-projection` is the asynchronous reservation audit projection service
for `PAY_ALL`.

The service consumes reservation lifecycle events from Kafka topic
`pay-limit-engine-reservation-events` and writes durable audit/reporting state to
Postgres schema `pay_limit_projection`. It is not part of the online limit
decision path: `pay-limit-engine` does not read this projection when evaluating,
holding, confirming, or rolling back operations.

The service exposes an internal read API at `/internal/v1/limit-projection/**`
guarded by `X-Internal-Admin-Key`. Aggregates are computed at read time from the
projection tables.

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

## Enriched Event Contract

The projection consumes `ReservationEvent` JSON messages from
`pay-limit-engine-reservation-events`. Each message carries:

| Field | Type | Description |
| --- | --- | --- |
| `eventId` | UUID | Unique event identifier. Used for idempotent deduplication. |
| `eventType` | Enum | `ReservationHeld`, `ReservationConfirmed`, `ReservationRolledBack`. |
| `occurredAt` | Instant | UTC time the lifecycle change occurred in the engine. |
| `reservationId` | UUID | Reservation this event belongs to. |
| `operationId` | String | Caller-supplied idempotency key for the original operation. |
| `state` | Enum | Reservation state after this event: `HELD`, `CONFIRMED`, `ROLLED_BACK`. |
| `staleAfter` | Instant | UTC deadline after which the reservation is considered stale (nullable). |
| `merchantId` | String | Merchant identifier (enriched by the engine at write time). |
| `operationType` | String | Limit operation type code (enriched). |
| `direction` | String | `IN` or `OUT` (enriched). |
| `amount` | BigDecimal | Monetary amount subject to the reservation (enriched). |
| `currency` | String | ISO-4217 currency code (enriched). |
| `reasons` | JSON array | Limit decision reason codes (from evaluation). |
| `matchedRules` | JSON array | Matched rule identifiers from the active manifest. |

The `merchantId`, `operationType`, `direction`, `amount`, and `currency` fields
were added by enrichment in `pay-limit-engine` (Task 1) to enable read-time
reporting without joining back to the engine or management service.

Kafka delivery is at-least-once. The projection deduplicates by `eventId` before
writing. Out-of-order delivery is handled: `reservation_state` is only updated
when the incoming `occurredAt` is later than the stored `last_occurred_at`.

## Schema

Flyway migrations live under `src/main/resources/db/migration/`.

### `reservation_event` (append-only event log)

| Column | Type | Description |
| --- | --- | --- |
| `event_id` | uuid PK | Unique event id; primary deduplication key. |
| `reservation_id` | uuid | Identifies the reservation. |
| `operation_id` | text | Caller operation key. |
| `event_type` | text | `ReservationHeld`, `ReservationConfirmed`, `ReservationRolledBack`. |
| `state` | text | Reservation state after the event. |
| `occurred_at` | timestamptz | When the event occurred in the engine. |
| `merchant_id` | text | Enriched merchant identifier. |
| `operation_type` | text | Enriched operation type. |
| `direction` | text | Enriched direction. |
| `amount` | numeric(38,2) | Enriched amount. |
| `currency` | text | Enriched currency. |
| `reasons` | jsonb | Limit decision reason codes. |
| `matched_rules` | jsonb | Matched rule ids. |
| `payload_json` | jsonb | Engine-internal payload snapshot. |
| `received_at` | timestamptz | Wall-clock time the projection received the message. |
| `kafka_topic` | text | Source Kafka topic. |
| `kafka_partition` | integer | Source Kafka partition. |
| `kafka_offset` | bigint | Source Kafka offset. |

Indexes: `(reservation_id, occurred_at)`, `(operation_id)`,
`(merchant_id, occurred_at)`.

### `reservation_state` (latest-known state per reservation)

| Column | Type | Description |
| --- | --- | --- |
| `reservation_id` | uuid PK | Identifies the reservation. |
| `operation_id` | text | Caller operation key. |
| `state` | text | Current state. |
| `merchant_id` | text | Enriched merchant identifier. |
| `operation_type` | text | Enriched operation type. |
| `direction` | text | Enriched direction. |
| `amount` | numeric(38,2) | Enriched amount. |
| `currency` | text | Enriched currency. |
| `held_at` | timestamptz | Time the first `HELD` event was seen (nullable). |
| `last_event_id` | uuid | Most recent event id applied. |
| `last_event_type` | text | Most recent event type. |
| `last_occurred_at` | timestamptz | `occurredAt` of the most recent event applied. |
| `stale_after` | timestamptz | Engine-set stale deadline (nullable). |
| `updated_at` | timestamptz | Wall-clock time this row was last written. |

Index: `(merchant_id, last_occurred_at)`.

## Read API

All endpoints are under `/internal/v1/limit-projection/` and require the
`X-Internal-Admin-Key` header matching `INTERNAL_ADMIN_API_KEY` (when
`INTERNAL_ADMIN_API_KEY_REQUIRED=true`). Responses use the shared
`{ data, meta, error, timestamp }` envelope.

### `GET /internal/v1/limit-projection/reservations/{reservationId}`

Returns the latest-known state for a reservation by its UUID.
Returns `404 RESERVATION_NOT_FOUND` when not found.

### `GET /internal/v1/limit-projection/operations/{operationId}/reservation`

Returns the latest-known state for the reservation associated with a caller
operation id. Returns `404 RESERVATION_NOT_FOUND` when not found.

### `GET /internal/v1/limit-projection/reservations`

Paginated list of reservation states. Optional query parameters:

| Parameter | Description |
| --- | --- |
| `merchantId` | Filter by merchant. |
| `state` | Filter by state (`HELD`, `CONFIRMED`, `ROLLED_BACK`). |
| `from` | ISO-8601 UTC lower bound on `last_occurred_at`. |
| `to` | ISO-8601 UTC upper bound on `last_occurred_at`. |
| `page` | Zero-based page number (default `0`). |
| `size` | Page size (default `50`, max `200`). |

### `GET /internal/v1/limit-projection/reservations/{reservationId}/events`

Returns all append-only `reservation_event` rows for a given reservation,
ordered by `occurred_at` ascending.

### `GET /internal/v1/limit-projection/reservations/summary`

Returns a read-time aggregate summary. Aggregates are computed at query time
from `reservation_state` (rows where `state = 'CONFIRMED'`). Optional query parameters:

| Parameter | Description |
| --- | --- |
| `merchantId` | Scope to a single merchant. |
| `from` | ISO-8601 UTC lower bound on `last_occurred_at`. |
| `to` | ISO-8601 UTC upper bound on `last_occurred_at`. |
| `groupBy` | Grouping key: `day` or `merchant` (default). |

Returns a list of `{ groupKey, confirmedCount, confirmedAmount, currency }` rows.

## Configuration

| Property | Environment variable | Default | Purpose |
| --- | --- | --- | --- |
| `server.port` | `SERVER_PORT` | `8088` | HTTP and actuator port (compose uses `8090`). |
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
| `pay-limit-projection.internal-admin.api-key` | `INTERNAL_ADMIN_API_KEY` | empty | Shared internal admin key for `/internal/**` endpoints. |
| `pay-limit-projection.internal-admin.api-key-required` | `INTERNAL_ADMIN_API_KEY_REQUIRED` | `false` | Fail requests without the key when `true`. |

The database password is a secret and belongs in Vault at:

```text
pay/{environment}/pay-limit-projection-db-password
```

The Vault secret should store the target Spring property key, for example:

```properties
spring.datasource.password=<secret>
```

## Compose Contour

The service runs at host port **8090** in the Docker Compose development contour
(the `pay-limit-projection` service in `infra/docker-compose.yaml`). Port 8088
is reserved for `sbp-authpay-mock`.

Key compose environment variables:

```
SERVER_PORT=8090
KAFKA_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_TOPIC=pay-limit-engine-reservation-events
KAFKA_GROUP_ID=pay-limit-projection
FLYWAY_ENABLED=true
PAY_LIMIT_PROJECTION_DB_URL=jdbc:postgresql://postgres:5432/pay_admin?currentSchema=pay_limit_projection
PAY_LIMIT_PROJECTION_DB_USERNAME=pay_admin
VAULT_ENABLED=true  # secret from pay/compose/pay-limit-projection-db-password
INTERNAL_ADMIN_API_KEY=${INTERNAL_ADMIN_API_KEY}
INTERNAL_ADMIN_API_KEY_REQUIRED=true
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
