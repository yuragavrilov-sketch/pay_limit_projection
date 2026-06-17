package ru.copperside.paylimits.projection.reservation.application;

public record KafkaCoordinates(String topic, int partition, long offset) {
}
