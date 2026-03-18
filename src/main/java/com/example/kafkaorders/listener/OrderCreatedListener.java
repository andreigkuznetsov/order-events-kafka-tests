package com.example.kafkaorders.listener;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.service.OrderProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private final OrderProcessingService processingService;

    public OrderCreatedListener(OrderProcessingService processingService) {
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.orders-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(OrderCreatedEvent event) {
        processingService.process(event);
    }
}
