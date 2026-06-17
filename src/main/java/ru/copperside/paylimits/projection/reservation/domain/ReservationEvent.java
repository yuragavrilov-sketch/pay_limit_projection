package ru.copperside.paylimits.projection.reservation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReservationEvent(
        UUID eventId,
        UUID reservationId,
        String operationId,
        ReservationEventType eventType,
        ReservationState state,
        Instant occurredAt,
        String merchantId,
        String operationType,
        String direction,
        BigDecimal amount,
        String currency,
        Instant staleAfter,
        String reasonsJson,
        String matchedRulesJson,
        String payloadJson
) {
}
