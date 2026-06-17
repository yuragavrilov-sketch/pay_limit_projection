package ru.copperside.paylimits.projection.reservation.application;

import org.junit.jupiter.api.Test;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEvent;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEventType;
import ru.copperside.paylimits.projection.reservation.domain.ReservationState;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationEventParserTest {

    private final ReservationEventParser parser = new ReservationEventParser();

    private static final String VALID = """
            {"eventId":"0f0e2d2a-1111-4a2b-9c3d-000000000001","eventType":"ReservationConfirmed",
             "occurredAt":"2026-05-29T12:05:00Z","reservationId":"11111111-1111-1111-1111-111111111111",
             "operationId":"op-1","state":"CONFIRMED","merchantId":"502118","operationType":"SBP_C2B",
             "direction":"IN","amount":"250.00","currency":"RUB","staleAfter":"2026-05-29T12:15:00Z",
             "reasons":[],"matchedRules":[{"ruleCode":"RULE_DAILY_AMOUNT"}]}
            """;

    @Test
    void parsesValidEvent() {
        ReservationEvent event = parser.parse(VALID);

        assertThat(event.eventType()).isEqualTo(ReservationEventType.RESERVATION_CONFIRMED);
        assertThat(event.state()).isEqualTo(ReservationState.CONFIRMED);
        assertThat(event.merchantId()).isEqualTo("502118");
        assertThat(event.amount()).isEqualByComparingTo("250.00");
        assertThat(event.currency()).isEqualTo("RUB");
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-05-29T12:05:00Z"));
        assertThat(event.matchedRulesJson()).contains("RULE_DAILY_AMOUNT");
        assertThat(event.payloadJson()).contains("\"operationId\":\"op-1\"");
    }

    @Test
    void rejectsMissingRequiredField() {
        String missingMerchant = VALID.replace("\"merchantId\":\"502118\",", "");
        assertThatThrownBy(() -> parser.parse(missingMerchant))
                .isInstanceOf(InvalidReservationEventException.class);
    }

    @Test
    void rejectsUnknownEventType() {
        String badType = VALID.replace("ReservationConfirmed", "ReservationExploded");
        assertThatThrownBy(() -> parser.parse(badType))
                .isInstanceOf(InvalidReservationEventException.class);
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> parser.parse("{not json"))
                .isInstanceOf(InvalidReservationEventException.class);
    }
}
