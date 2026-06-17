package ru.copperside.paylimits.projection.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemEnvelope> handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.code(), "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemEnvelope> handleBadRequest(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", ex.getMessage());
    }

    private ResponseEntity<ProblemEnvelope> problem(HttpStatus status, String code, String title, String message) {
        ProblemDetail detail = new ProblemDetail(
                "https://contracts.newpay/errors/" + code.toLowerCase().replace('_', '-'),
                title, status.value(), code, message, null,
                "00000000-0000-0000-0000-000000000000");
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemEnvelope.of(detail, clock));
    }
}
