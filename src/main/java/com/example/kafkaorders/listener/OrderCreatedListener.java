package com.example.kafkaorders.listener;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.exception.OrderValidationException;
import com.example.kafkaorders.service.OrderProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final OrderProcessingService processingService;

    public OrderCreatedListener(OrderProcessingService processingService) {
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.orders-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent, eventId={}, orderId={}", event.eventId(), event.orderId());
        processingService.process(event);
    }
}