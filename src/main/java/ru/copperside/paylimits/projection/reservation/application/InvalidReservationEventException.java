package ru.copperside.paylimits.projection.reservation.application;

public class InvalidReservationEventException extends RuntimeException {
    public InvalidReservationEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidReservationEventException(String message) {
        super(message);
    }
}
