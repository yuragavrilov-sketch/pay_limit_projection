package ru.copperside.paylimits.projection.reservation.application.view;

import java.time.Instant;

public record ReservationEventView(
        String eventId, String eventType, String state, Instant occurredAt, String amount, String currency) {
}
