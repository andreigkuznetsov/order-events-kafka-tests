package com.example.kafkaorders.dto;

import java.time.Instant;

public record OrderFailedEvent(
        String eventId,
        String orderId,
        String reason,
        Instant failedAt
) {
}
