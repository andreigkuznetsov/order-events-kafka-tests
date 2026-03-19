package com.example.kafkaorders.service;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.dto.OrderFailedEvent;
import com.example.kafkaorders.dto.OrderProcessedEvent;
import com.example.kafkaorders.entity.ProcessedOrderEntity;
import com.example.kafkaorders.exception.OrderValidationException;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OrderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    private final OrderValidationService validationService;
    private final TechnicalFailureSimulationService technicalFailureSimulationService;
    private final ProcessedOrderRepository repository;
    private final OrderEventPublisher publisher;

    public OrderProcessingService(
            OrderValidationService validationService,
            TechnicalFailureSimulationService technicalFailureSimulationService,
            ProcessedOrderRepository repository,
            OrderEventPublisher publisher
    ) {
        this.validationService = validationService;
        this.technicalFailureSimulationService = technicalFailureSimulationService;
        this.repository = repository;
        this.publisher = publisher;
    }

    public void process(OrderCreatedEvent event) {
        log.info("Start processing eventId={}, orderId={}", event.eventId(), event.orderId());

        try {
            validationService.validate(event);
        } catch (OrderValidationException ex) {
            OrderFailedEvent failedEvent = new OrderFailedEvent(
                    event.eventId(),
                    event.orderId(),
                    ex.getMessage(),
                    Instant.now()
            );
            publisher.publishFailed(failedEvent);
            log.error("Validation failed, orderId={}, reason={}", event.orderId(), ex.getMessage());
            return;
        }

        if (repository.existsByEventId(event.eventId())) {
            log.warn("Duplicate event skipped, eventId={}", event.eventId());
            return;
        }

        technicalFailureSimulationService.check(event);

        try {
            ProcessedOrderEntity entity = new ProcessedOrderEntity(
                    event.eventId(),
                    event.orderId(),
                    event.userId(),
                    event.amount(),
                    event.currency(),
                    Instant.now()
            );

            repository.save(entity);
            log.info("Order saved to DB, orderId={}", event.orderId());

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
            log.info("Published processed event, orderId={}", event.orderId());
        } catch (DataIntegrityViolationException ex) {
            if (repository.existsByEventId(event.eventId())) {
                log.warn("Duplicate event detected after DB race, eventId={}", event.eventId());
                return;
            }
            throw ex;
        }
    }
}