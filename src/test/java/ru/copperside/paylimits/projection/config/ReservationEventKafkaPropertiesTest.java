package ru.copperside.paylimits.projection.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationEventKafkaPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsKafkaProperties() {
        contextRunner
                .withPropertyValues(
                        "pay-limit-projection.kafka.enabled=true",
                        "pay-limit-projection.kafka.bootstrap-servers=kafka:9092",
                        "pay-limit-projection.kafka.topic=pay-limit-engine-reservation-events",
                        "pay-limit-projection.kafka.group-id=pay-limit-projection")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ReservationEventKafkaProperties properties = context.getBean(ReservationEventKafkaProperties.class);
                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.bootstrapServers()).isEqualTo("kafka:9092");
                    assertThat(properties.topic()).isEqualTo("pay-limit-engine-reservation-events");
                    assertThat(properties.groupId()).isEqualTo("pay-limit-projection");
                });
    }

    @Test
    void rejectsBlankValuesWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "pay-limit-projection.kafka.enabled=true",
                        "pay-limit-projection.kafka.bootstrap-servers=",
                        "pay-limit-projection.kafka.topic=",
                        "pay-limit-projection.kafka.group-id=")
                .run(context -> assertThat(context).hasFailed());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ReservationEventKafkaProperties.class)
    static class TestConfig {
    }
}
