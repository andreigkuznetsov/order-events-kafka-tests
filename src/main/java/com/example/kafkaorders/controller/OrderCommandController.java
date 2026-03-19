package com.example.kafkaorders.controller;

import com.example.kafkaorders.dto.CreateOrderRequest;
import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.support.TopicNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Operations for order publishing")
public class OrderCommandController {

    private static final Logger log = LoggerFactory.getLogger(OrderCommandController.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TopicNames topicNames;

    public OrderCommandController(KafkaTemplate<String, Object> kafkaTemplate, TopicNames topicNames) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Publishes order event to Kafka")
    public ResponseEntity<String> createOrder(@Valid @RequestBody CreateOrderRequest request) throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                request.orderId(),
                request.userId(),
                request.amount(),
                request.currency(),
                Instant.now()
        );

        log.info("Received REST request, orderId={}", request.orderId());

        kafkaTemplate.send(topicNames.ordersCreated(), event.orderId(), event)
                .get(5, TimeUnit.SECONDS);

        return ResponseEntity.accepted().body("Order event published");
    }
}