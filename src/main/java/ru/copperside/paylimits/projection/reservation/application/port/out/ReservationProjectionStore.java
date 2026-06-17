package ru.copperside.paylimits.projection.reservation.application.port.out;

import ru.copperside.paylimits.projection.reservation.application.KafkaCoordinates;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEvent;

public interface ReservationProjectionStore {

    /**
     * Persists the event and updates current state idempotently.
     *
     * @return true if the event was newly applied, false if it was a duplicate eventId.
     */
    boolean apply(ReservationEvent event, KafkaCoordinates coordinates);
}
