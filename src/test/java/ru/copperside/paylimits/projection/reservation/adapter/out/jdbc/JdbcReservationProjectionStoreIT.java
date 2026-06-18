package ru.copperside.paylimits.projection.reservation.adapter.out.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.copperside.paylimits.projection.reservation.application.KafkaCoordinates;
import ru.copperside.paylimits.projection.reservation.application.port.out.ReservationProjectionStore;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEvent;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEventType;
import ru.copperside.paylimits.projection.reservation.domain.ReservationState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("pgtest")
class JdbcReservationProjectionStoreIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl() + "&currentSchema=pay_limit_projection");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    ReservationProjectionStore store;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private ReservationEvent event(UUID eventId, UUID reservationId, ReservationEventType type, ReservationState state, Instant occurredAt) {
        return new ReservationEvent(eventId, reservationId, "op-" + reservationId, type, state, occurredAt,
                "502118", "SBP_C2B", "IN", new BigDecimal("250.00"), "RUB", null, "[]", "[]",
                "{\"eventId\":\"" + eventId + "\"}");
    }

    @Test
    void appliesNewEventAndUpsertsState() {
        UUID reservationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        boolean applied = store.apply(
                event(eventId, reservationId, ReservationEventType.RESERVATION_HELD, ReservationState.HELD, Instant.parse("2026-05-29T12:00:00Z")),
                new KafkaCoordinates("t", 0, 1L));

        assertThat(applied).isTrue();
        assertThat(jdbcTemplate.queryForObject("select state from reservation_state where reservation_id = ?", String.class, reservationId))
                .isEqualTo("HELD");
        assertThat(jdbcTemplate.queryForObject("select count(*) from reservation_event where event_id = ?", Integer.class, eventId))
                .isEqualTo(1);
    }

    @Test
    void ignoresDuplicateEventId() {
        UUID reservationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        ReservationEvent held = event(eventId, reservationId, ReservationEventType.RESERVATION_HELD, ReservationState.HELD, Instant.parse("2026-05-29T12:00:00Z"));

        assertThat(store.apply(held, new KafkaCoordinates("t", 0, 1L))).isTrue();
        assertThat(store.apply(held, new KafkaCoordinates("t", 0, 1L))).isFalse();

        assertThat(jdbcTemplate.queryForObject("select count(*) from reservation_event where reservation_id = ?", Integer.class, reservationId))
                .isEqualTo(1);
    }

    @Test
    void doesNotOverwriteNewerStateWithOlderEvent() {
        UUID reservationId = UUID.randomUUID();
        store.apply(event(UUID.randomUUID(), reservationId, ReservationEventType.RESERVATION_CONFIRMED, ReservationState.CONFIRMED, Instant.parse("2026-05-29T12:05:00Z")),
                new KafkaCoordinates("t", 0, 2L));
        // older Held event arrives late
        store.apply(event(UUID.randomUUID(), reservationId, ReservationEventType.RESERVATION_HELD, ReservationState.HELD, Instant.parse("2026-05-29T12:00:00Z")),
                new KafkaCoordinates("t", 0, 3L));

        assertThat(jdbcTemplate.queryForObject("select state from reservation_state where reservation_id = ?", String.class, reservationId))
                .isEqualTo("CONFIRMED");
    }
}
