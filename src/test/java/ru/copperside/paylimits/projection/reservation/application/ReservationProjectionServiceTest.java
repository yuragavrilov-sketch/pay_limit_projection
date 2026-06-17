package ru.copperside.paylimits.projection.reservation.application;

import org.junit.jupiter.api.Test;
import ru.copperside.paylimits.projection.reservation.application.port.out.ReservationProjectionStore;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEvent;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationProjectionServiceTest {

    private static final String VALID = """
            {"eventId":"0f0e2d2a-1111-4a2b-9c3d-000000000001","eventType":"ReservationHeld",
             "occurredAt":"2026-05-29T12:00:00Z","reservationId":"11111111-1111-1111-1111-111111111111",
             "operationId":"op-1","state":"HELD","merchantId":"502118","operationType":"SBP_C2B",
             "direction":"IN","amount":"250.00","currency":"RUB","reasons":[],"matchedRules":[]}
            """;

    private final List<ReservationEvent> applied = new ArrayList<>();
    private final ReservationProjectionStore store = (event, coordinates) -> {
        applied.add(event);
        return true;
    };
    private final ReservationProjectionService service =
            new ReservationProjectionService(new ReservationEventParser(), store);

    @Test
    void ingestsValidEvent() {
        boolean result = service.ingest(VALID, new KafkaCoordinates("t", 0, 1L));

        assertThat(result).isTrue();
        assertThat(applied).hasSize(1);
        assertThat(applied.getFirst().merchantId()).isEqualTo("502118");
    }

    @Test
    void propagatesInvalidEvent() {
        assertThatThrownBy(() -> service.ingest("{bad", new KafkaCoordinates("t", 0, 1L)))
                .isInstanceOf(InvalidReservationEventException.class);
        assertThat(applied).isEmpty();
    }
}
