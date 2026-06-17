package ru.copperside.paylimits.projection.common.web;

import java.time.Clock;
import java.time.Instant;

public record ProblemEnvelope(ProblemDetail error, Instant timestamp) {
    public static ProblemEnvelope of(ProblemDetail error, Clock clock) {
        return new ProblemEnvelope(error, Instant.now(clock));
    }
}
