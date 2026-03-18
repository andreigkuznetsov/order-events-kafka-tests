package com.example.kafkaorders.service;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;

@Service
public class OrderValidationService {

    private static final Set<String> ALLOWED_CURRENCIES = Set.of("RUB", "USD", "EUR");

    public void validate(OrderCreatedEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (event.orderId() == null || event.orderId().isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (event.userId() == null || event.userId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (event.amount() == null || event.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (event.currency() == null || !ALLOWED_CURRENCIES.contains(event.currency())) {
            throw new IllegalArgumentException("currency is not supported");
        }
        if (event.createdAt() == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }
}