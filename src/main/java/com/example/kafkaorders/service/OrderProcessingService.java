package com.example.kafkaorders.service;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.dto.OrderFailedEvent;
import com.example.kafkaorders.dto.OrderProcessedEvent;
import com.example.kafkaorders.entity.ProcessedOrderEntity;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@Service
public class OrderProcessingService {

    private final OrderValidationService validationService;
    private final ProcessedOrderRepository repository;
    private final OrderEventPublisher publisher;
    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);


    public OrderProcessingService(
            OrderValidationService validationService,
            ProcessedOrderRepository repository,
            OrderEventPublisher publisher
    ) {
        this.validationService = validationService;
        this.repository = repository;
        this.publisher = publisher;
    }

    public void process(OrderCreatedEvent event) {
        try {
            log.info("Start processing event: eventId={}, orderId={}", event.eventId(), event.orderId());
            validationService.validate(event);

            if (repository.existsByEventId(event.eventId())) {
                log.warn("Duplicate event detected, skipping: eventId={}", event.eventId());
                return;
            }

            ProcessedOrderEntity entity = new ProcessedOrderEntity(
                    event.eventId(),
                    event.orderId(),
                    event.userId(),
                    event.amount(),
                    event.currency(),
                    Instant.now()
            );

            repository.save(entity);
            log.info("Order saved to DB: orderId={}", event.orderId());

            OrderProcessedEvent processedEvent = new OrderProcessedEvent(
                    event.eventId(),
                    event.orderId(),
                    event.userId(),
                    event.amount(),
                    event.currency(),
                    Instant.now(),
                    "PROCESSED"
            );

            publisher.publishProcessed(processedEvent);
            log.info("Published OrderProcessedEvent: orderId={}", event.orderId());
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            log.error("Order processing failed: orderId={}, reason={}", event.orderId(), ex.getMessage());
            OrderFailedEvent failedEvent = new OrderFailedEvent(
                    event.eventId(),
                    event.orderId(),
                    ex.getMessage(),
                    Instant.now()
            );
            publisher.publishFailed(failedEvent);
        }
    }
}
