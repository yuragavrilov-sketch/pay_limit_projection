package ru.copperside.paylimits.projection.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "pay-limit-projection.kafka", name = "enabled", havingValue = "true")
public class ReservationEventKafkaConfig {

    @Bean
    ConsumerFactory<String, String> reservationEventConsumerFactory(ReservationEventKafkaProperties properties) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.groupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> reservationEventKafkaListenerContainerFactory(
            ConsumerFactory<String, String> reservationEventConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reservationEventConsumerFactory);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
                new org.springframework.util.backoff.FixedBackOff(1000L, org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS)));
        return factory;
    }
}
