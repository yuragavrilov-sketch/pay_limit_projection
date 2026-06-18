package ru.copperside.paylimits.projection.reservation.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("pgtest")
class ReservationProjectionApiIT {

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

    @LocalServerPort
    int port;

    private RestClient rest;

    private UUID confirmedReservation;

    @BeforeEach
    void setup() {
        rest = RestClient.builder().baseUrl("http://localhost:" + port).build();
        jdbcTemplate.update("delete from reservation_event");
        jdbcTemplate.update("delete from reservation_state");
        confirmedReservation = UUID.randomUUID();
        store.apply(new ReservationEvent(UUID.randomUUID(), confirmedReservation, "op-confirmed",
                ReservationEventType.RESERVATION_CONFIRMED, ReservationState.CONFIRMED, Instant.parse("2026-05-29T12:05:00Z"),
                "502118", "SBP_C2B", "IN", new BigDecimal("250.00"), "RUB", null, "[]", "[]", "{}"),
                new KafkaCoordinates("t", 0, 1L));
    }

    @Test
    void getByReservationIdReturnsState() {
        ResponseEntity<String> response = rest.get()
                .uri("/internal/v1/limit-projection/reservations/" + confirmedReservation)
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"state\":\"CONFIRMED\"").contains("\"merchantId\":\"502118\"");
    }

    @Test
    void getUnknownReservationReturns404() {
        ResponseEntity<String> response = rest.get()
                .uri("/internal/v1/limit-projection/reservations/" + UUID.randomUUID())
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {})
                .toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains("RESERVATION_NOT_FOUND");
    }

    @Test
    void listByMerchantReturnsRow() {
        ResponseEntity<String> response = rest.get()
                .uri("/internal/v1/limit-projection/reservations?merchantId=502118")
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains(confirmedReservation.toString());
    }

    @Test
    void summaryByMerchantAggregatesConfirmedAmount() {
        ResponseEntity<String> response = rest.get()
                .uri("/internal/v1/limit-projection/reservations/summary?merchantId=502118&groupBy=merchant")
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"confirmedCount\":1").contains("250.00");
    }

    @Test
    void getByOperationIdReturnsState() {
        ResponseEntity<String> response = rest.get()
                .uri("/internal/v1/limit-projection/operations/op-confirmed/reservation")
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"state\":\"CONFIRMED\"");
    }

    @Test
    void dateRangeFilterBindsAndFilters() {
        ResponseEntity<String> response = rest.get()
                .uri("/internal/v1/limit-projection/reservations?merchantId=502118&from=2026-05-29T00:00:00Z&to=2026-05-30T00:00:00Z")
                .retrieve()
                .onStatus(status -> status.value() != 200, (req, res) -> {
                    throw new AssertionError("Expected 200 but got " + res.getStatusCode() + ": " + new String(res.getBody().readAllBytes()));
                })
                .toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains(confirmedReservation.toString());
    }
}
