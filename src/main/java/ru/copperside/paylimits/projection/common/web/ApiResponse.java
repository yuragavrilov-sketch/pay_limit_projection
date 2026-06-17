package ru.copperside.paylimits.projection.common.web;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(T data, Object meta, Object error, Instant timestamp) {
    public static <T> ApiResponse<T> success(T data, Clock clock) {
        return new ApiResponse<>(data, Map.of(), null, Instant.now(clock));
    }
}
