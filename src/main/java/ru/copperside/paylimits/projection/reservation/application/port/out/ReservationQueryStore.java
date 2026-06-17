package ru.copperside.paylimits.projection.reservation.application.port.out;

import ru.copperside.paylimits.projection.reservation.application.view.ReservationEventView;
import ru.copperside.paylimits.projection.reservation.application.view.ReservationStateView;
import ru.copperside.paylimits.projection.reservation.application.view.ReservationSummaryRow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationQueryStore {
    Optional<ReservationStateView> findByReservationId(UUID reservationId);
    Optional<ReservationStateView> findByOperationId(String operationId);
    List<ReservationStateView> list(String merchantId, String state, Instant from, Instant to, int page, int size);
    List<ReservationEventView> events(UUID reservationId);
    List<ReservationSummaryRow> summary(String merchantId, Instant from, Instant to, String groupBy);
}
