package com.dataflow.export.service;

import com.dataflow.export.domain.OutboxEvent;
import com.dataflow.export.event.ExportRequestEvent;
import com.dataflow.export.messaging.ExportKafkaProducer;
import com.dataflow.export.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final ExportKafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox Poller: Found {} PENDING events to publish to Kafka", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                ExportRequestEvent exportEvent = objectMapper.readValue(event.getPayload(), ExportRequestEvent.class);
                kafkaProducer.sendExportEvent(exportEvent);

                event.setStatus("PUBLISHED");
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.info("Outbox Event [{}] marked as PUBLISHED", event.getId());
            } catch (Exception e) {
                log.error("Failed to process Outbox Event [{}]", event.getId(), e);
                event.setStatus("FAILED");
                outboxEventRepository.save(event);
            }
        }
    }
}
