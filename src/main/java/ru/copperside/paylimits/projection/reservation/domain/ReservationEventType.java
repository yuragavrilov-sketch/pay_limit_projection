package ru.copperside.paylimits.projection.reservation.domain;

public enum ReservationEventType {
    RESERVATION_HELD,
    RESERVATION_CONFIRMED,
    RESERVATION_ROLLED_BACK;

    public static ReservationEventType fromWire(String wire) {
        return switch (wire) {
            case "ReservationHeld" -> RESERVATION_HELD;
            case "ReservationConfirmed" -> RESERVATION_CONFIRMED;
            case "ReservationRolledBack" -> RESERVATION_ROLLED_BACK;
            case null, default -> throw new IllegalArgumentException("Unknown eventType: " + wire);
        };
    }
}
