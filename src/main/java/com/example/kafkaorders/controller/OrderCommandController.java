package com.example.kafkaorders.controller;

import com.example.kafkaorders.dto.CreateOrderRequest;
import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.support.TopicNames;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/orders")
public class OrderCommandController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TopicNames topicNames;
    private static final Logger log = LoggerFactory.getLogger(OrderCommandController.class);

    public OrderCommandController(KafkaTemplate<String, Object> kafkaTemplate, TopicNames topicNames) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@Valid @RequestBody CreateOrderRequest request) throws Exception {
        log.info("Received REST request: orderId={}", request.orderId());
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                request.orderId(),
                request.userId(),
                request.amount(),
                request.currency(),
                Instant.now()
        );

        kafkaTemplate.send(topicNames.ordersCreated(), event.orderId(), event)
                .get(5, TimeUnit.SECONDS);

        return ResponseEntity.accepted().body("Order event published");
    }

    @RestController
    public class HomeController {

        @GetMapping("/")
        public String home() {
            return "Kafka Order Processing Service is running";
        }
    }
}