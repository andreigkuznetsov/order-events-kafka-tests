package com.example.kafkaorders.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderProcessedEvent(
        String eventId,
        String orderId,
        String userId,
        BigDecimal amount,
        String currency,
        Instant processedAt,
        String status
) {
}
