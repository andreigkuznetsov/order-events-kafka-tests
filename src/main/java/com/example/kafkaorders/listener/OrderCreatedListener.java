package com.example.kafkaorders.listener;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.monitoring.OrderMetricsService;
import com.example.kafkaorders.service.OrderProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final OrderProcessingService processingService;
    private final OrderMetricsService metricsService;

    public OrderCreatedListener(
            OrderProcessingService processingService,
            OrderMetricsService metricsService
    ) {
        this.processingService = processingService;
        this.metricsService = metricsService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.orders-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(OrderCreatedEvent event) {
        long startedAt = System.currentTimeMillis();

        log.info("Received OrderCreatedEvent, eventId={}, orderId={}", event.eventId(), event.orderId());
        metricsService.recordCreated();

        try {
            processingService.process(event);
        } finally {
            long durationMillis = System.currentTimeMillis() - startedAt;
            metricsService.recordProcessingTime(durationMillis);
        }
    }
}