package ru.copperside.paylimits.projection.reservation.application.view;

public record ReservationSummaryRow(String groupKey, long confirmedCount, String confirmedAmount, String currency) {
}
