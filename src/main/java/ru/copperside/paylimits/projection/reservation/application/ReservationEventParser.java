package ru.copperside.paylimits.projection.reservation.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEvent;
import ru.copperside.paylimits.projection.reservation.domain.ReservationEventType;
import ru.copperside.paylimits.projection.reservation.domain.ReservationState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class ReservationEventParser {

    // Self-contained mapper: the parser only reads a JSON tree, so it needs no
    // Spring-configured ObjectMapper (and the context has no ObjectMapper bean
    // to inject).
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReservationEvent parse(String json) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new InvalidReservationEventException("Reservation event is not valid JSON", exception);
        }
        try {
            ReservationEventType eventType = ReservationEventType.fromWire(requiredText(root, "eventType"));
            ReservationState state = ReservationState.valueOf(requiredText(root, "state"));
            return new ReservationEvent(
                    UUID.fromString(requiredText(root, "eventId")),
                    UUID.fromString(requiredText(root, "reservationId")),
                    requiredText(root, "operationId"),
                    eventType,
                    state,
                    Instant.parse(requiredText(root, "occurredAt")),
                    requiredText(root, "merchantId"),
                    requiredText(root, "operationType"),
                    requiredDirection(root),
                    new BigDecimal(requiredText(root, "amount")),
                    requiredText(root, "currency"),
                    optionalInstant(root, "staleAfter"),
                    arrayJson(root, "reasons"),
                    arrayJson(root, "matchedRules"),
                    json);
        } catch (InvalidReservationEventException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InvalidReservationEventException("Reservation event failed validation", exception);
        }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new InvalidReservationEventException("Missing required reservation event field: " + field);
        }
        return node.asText();
    }

    private String requiredDirection(JsonNode root) {
        String direction = requiredText(root, "direction");
        if (!direction.equals("IN") && !direction.equals("OUT")) {
            throw new InvalidReservationEventException("Unknown direction: " + direction);
        }
        return direction;
    }

    private Instant optionalInstant(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return Instant.parse(node.asText());
    }

    private String arrayJson(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return "[]";
        }
        return node.toString();
    }
}
