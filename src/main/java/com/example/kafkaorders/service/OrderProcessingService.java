package com.example.kafkaorders.service;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.dto.OrderFailedEvent;
import com.example.kafkaorders.dto.OrderProcessedEvent;
import com.example.kafkaorders.entity.ProcessedOrderEntity;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OrderProcessingService {

    private final OrderValidationService validationService;
    private final ProcessedOrderRepository repository;
    private final OrderEventPublisher publisher;

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
            validationService.validate(event);

            if (repository.existsByEventId(event.eventId())) {
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
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
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
