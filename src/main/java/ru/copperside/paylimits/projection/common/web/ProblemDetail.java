package ru.copperside.paylimits.projection.common.web;

public record ProblemDetail(
        String type, String title, int status, String code, String message, Object details, String traceId) {
}
