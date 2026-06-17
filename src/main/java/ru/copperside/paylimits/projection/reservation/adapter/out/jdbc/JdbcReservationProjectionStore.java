package ru.copperside.paylimits.projection.reservation.adapter.out.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.copperside.paylimits.projection.reservation.application.KafkaCoordinates;
import ru.copperside.paylimits.projection.reservation.application.port.out.ReservationProjectionStore;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEvent;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEventType;
import ru.copperside.paylimits.projection.reservation.domain.ReservationState;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

@Repository
public class JdbcReservationProjectionStore implements ReservationProjectionStore {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcReservationProjectionStore(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean apply(ReservationEvent event, KafkaCoordinates coordinates) {
        Timestamp now = Timestamp.from(Instant.now(clock));
        int inserted = jdbcTemplate.update("""
                insert into reservation_event (
                    event_id, reservation_id, operation_id, event_type, state, occurred_at,
                    merchant_id, operation_type, direction, amount, currency,
                    reasons, matched_rules, payload_json, received_at,
                    kafka_topic, kafka_partition, kafka_offset)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), cast(? as jsonb), ?, ?, ?, ?)
                on conflict (event_id) do nothing
                """,
                event.eventId(), event.reservationId(), event.operationId(),
                event.eventType().name(), event.state().name(), Timestamp.from(event.occurredAt()),
                event.merchantId(), event.operationType(), event.direction(), event.amount(), event.currency(),
                event.reasonsJson(), event.matchedRulesJson(), event.payloadJson(), now,
                coordinates.topic(), coordinates.partition(), coordinates.offset());

        if (inserted == 0) {
            return false;
        }

        Timestamp heldAt = event.eventType() == ReservationEventType.RESERVATION_HELD
                ? Timestamp.from(event.occurredAt())
                : null;
        Timestamp staleAfter = event.staleAfter() == null ? null : Timestamp.from(event.staleAfter());

        jdbcTemplate.update("""
                insert into reservation_state (
                    reservation_id, operation_id, state, merchant_id, operation_type, direction, amount, currency,
                    held_at, last_event_id, last_event_type, last_occurred_at, stale_after, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (reservation_id) do update set
                    state = excluded.state,
                    operation_id = excluded.operation_id,
                    merchant_id = excluded.merchant_id,
                    operation_type = excluded.operation_type,
                    direction = excluded.direction,
                    amount = excluded.amount,
                    currency = excluded.currency,
                    held_at = coalesce(reservation_state.held_at, excluded.held_at),
                    last_event_id = excluded.last_event_id,
                    last_event_type = excluded.last_event_type,
                    last_occurred_at = excluded.last_occurred_at,
                    stale_after = excluded.stale_after,
                    updated_at = excluded.updated_at
                where excluded.last_occurred_at >= reservation_state.last_occurred_at
                """,
                event.reservationId(), event.operationId(), event.state().name(),
                event.merchantId(), event.operationType(), event.direction(), event.amount(), event.currency(),
                heldAt, event.eventId(), event.eventType().name(), Timestamp.from(event.occurredAt()), staleAfter, now);

        return true;
    }
}
