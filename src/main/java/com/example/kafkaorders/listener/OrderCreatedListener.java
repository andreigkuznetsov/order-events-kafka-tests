package com.example.kafkaorders.listener;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.service.OrderProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OrderCreatedListener {

    private final OrderProcessingService processingService;
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    public OrderCreatedListener(OrderProcessingService processingService) {
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.orders-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(OrderCreatedEvent event) {
        log.info("Processing order event: orderId={}, eventId={}", event.orderId(), event.eventId());
        processingService.process(event);
    }
}
