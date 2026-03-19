package com.example.kafkaorders.support;

import com.example.kafkaorders.dto.CreateOrderRequest;
import com.example.kafkaorders.dto.OrderCreatedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class OrderEventFactory {

    private OrderEventFactory() {
    }

    public static OrderCreatedEvent validOrder() {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "ORD-" + UUID.randomUUID(),
                "USER-100",
                new BigDecimal("1500.00"),
                "RUB",
                Instant.now()
        );
    }

    public static OrderCreatedEvent invalidWithoutOrderId() {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                null,
                "USER-100",
                new BigDecimal("1500.00"),
                "RUB",
                Instant.now()
        );
    }

    public static CreateOrderRequest validCreateOrderRequest() {
        return new CreateOrderRequest(
                "ORD-" + UUID.randomUUID(),
                "USER-100",
                new BigDecimal("1500.00"),
                "RUB"
        );
    }

    public static OrderCreatedEvent transientFailureOrder() {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "ORD-RETRY-" + UUID.randomUUID(),
                "FORCE_RETRY",
                new BigDecimal("1500.00"),
                "RUB",
                Instant.now()
        );
    }
}