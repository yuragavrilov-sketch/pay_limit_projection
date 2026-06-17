package ru.copperside.paylimits.projection.reservation.application.view;

import java.time.Instant;

public record ReservationStateView(
        String reservationId, String operationId, String state, String merchantId,
        String operationType, String direction, String amount, String currency,
        Instant heldAt, Instant lastOccurredAt, Instant staleAfter) {
}
