package ru.copperside.paylimits.projection.reservation.adapter.in.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.copperside.paylimits.projection.reservation.application.KafkaCoordinates;
import ru.copperside.paylimits.projection.reservation.application.ReservationProjectionService;

@Component
@ConditionalOnProperty(prefix = "pay-limit-projection.kafka", name = "enabled", havingValue = "true")
public class ReservationEventListener {

    private final ReservationProjectionService service;

    public ReservationEventListener(ReservationProjectionService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = "${pay-limit-projection.kafka.topic}",
            groupId = "${pay-limit-projection.kafka.group-id}",
            containerFactory = "reservationEventKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        service.ingest(record.value(), new KafkaCoordinates(record.topic(), record.partition(), record.offset()));
    }
}
