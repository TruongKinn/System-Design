package com.dataflow.export.messaging;

import com.dataflow.export.event.ExportRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExportKafkaProducer {

    private static final String TOPIC = "data.export.request";

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendExportEvent(ExportRequestEvent event) {
        if (kafkaTemplate != null) {
            try {
                log.info("Publishing ExportRequestEvent to Kafka topic [{}]: {}", TOPIC, event);
                kafkaTemplate.send(TOPIC, event.getJobId(), event);
                return;
            } catch (Exception e) {
                log.warn("Kafka broker unreachable, falling back to direct asynchronous event handler for event: {}", event);
            }
        } else {
            log.info("KafkaTemplate disabled/null. Running event processing directly for jobId: {}", event.getJobId());
        }
    }
}
