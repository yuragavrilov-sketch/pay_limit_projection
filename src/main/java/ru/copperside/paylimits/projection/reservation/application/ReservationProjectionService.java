package ru.copperside.paylimits.projection.reservation.application;

import org.springframework.stereotype.Service;
import ru.copperside.paylimits.projection.reservation.application.port.out.ReservationProjectionStore;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEvent;

import java.util.Objects;

@Service
public class ReservationProjectionService {

    private final ReservationEventParser parser;
    private final ReservationProjectionStore store;

    public ReservationProjectionService(ReservationEventParser parser, ReservationProjectionStore store) {
        this.parser = Objects.requireNonNull(parser);
        this.store = Objects.requireNonNull(store);
    }

    public boolean ingest(String json, KafkaCoordinates coordinates) {
        ReservationEvent event = parser.parse(json);
        return store.apply(event, coordinates);
    }
}
