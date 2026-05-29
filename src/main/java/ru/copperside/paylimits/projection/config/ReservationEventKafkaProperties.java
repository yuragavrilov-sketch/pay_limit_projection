package ru.copperside.paylimits.projection.config;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pay-limit-projection.kafka")
public record ReservationEventKafkaProperties(
        boolean enabled,
        String bootstrapServers,
        String topic,
        String groupId
) {

    @AssertTrue(message = "bootstrapServers, topic, and groupId must be present when Kafka is enabled")
    boolean isValidWhenEnabled() {
        return !enabled
                || (hasText(bootstrapServers) && hasText(topic) && hasText(groupId));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
