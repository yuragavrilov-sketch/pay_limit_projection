package ru.copperside.paylimits.projection.reservation.adapter.in.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.paylimits.projection.common.web.ApiResponse;
import ru.copperside.paylimits.projection.common.web.NotFoundException;
import ru.copperside.paylimits.projection.reservation.application.port.out.ReservationQueryStore;
import ru.copperside.paylimits.projection.reservation.application.view.ReservationEventView;
import ru.copperside.paylimits.projection.reservation.application.view.ReservationStateView;
import ru.copperside.paylimits.projection.reservation.application.view.ReservationSummaryRow;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/limit-projection")
public class ReservationProjectionController {

    private final ReservationQueryStore queryStore;
    private final Clock clock;

    public ReservationProjectionController(ReservationQueryStore queryStore, Clock clock) {
        this.queryStore = queryStore;
        this.clock = clock;
    }

    @GetMapping("/reservations/{reservationId}")
    ApiResponse<ReservationStateView> byReservationId(@PathVariable UUID reservationId) {
        return ApiResponse.success(queryStore.findByReservationId(reservationId)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "Reservation was not found")), clock);
    }

    @GetMapping("/operations/{operationId}/reservation")
    ApiResponse<ReservationStateView> byOperationId(@PathVariable String operationId) {
        return ApiResponse.success(queryStore.findByOperationId(operationId)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "Reservation was not found")), clock);
    }

    @GetMapping("/reservations")
    ApiResponse<List<ReservationStateView>> list(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(queryStore.list(merchantId, state, from, to, page, Math.min(size, 200)), clock);
    }

    @GetMapping("/reservations/{reservationId}/events")
    ApiResponse<List<ReservationEventView>> events(@PathVariable UUID reservationId) {
        return ApiResponse.success(queryStore.events(reservationId), clock);
    }

    @GetMapping("/reservations/summary")
    ApiResponse<List<ReservationSummaryRow>> summary(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "merchant") String groupBy) {
        return ApiResponse.success(queryStore.summary(merchantId, from, to, groupBy), clock);
    }
}
