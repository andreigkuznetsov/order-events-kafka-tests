package com.example.kafkaorders.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        String eventId,
        String orderId,
        String userId,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {
}
