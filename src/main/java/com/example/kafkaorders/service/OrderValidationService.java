package com.example.kafkaorders.service;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.exception.OrderValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class OrderValidationService {

    private static final Set<String> ALLOWED_CURRENCIES = Set.of("RUB", "USD", "EUR");

    public void validate(OrderCreatedEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new OrderValidationException("eventId is required");
        }
        if (event.orderId() == null || event.orderId().isBlank()) {
            throw new OrderValidationException("orderId is required");
        }
        if (event.userId() == null || event.userId().isBlank()) {
            throw new OrderValidationException("userId is required");
        }
        if (event.amount() == null || event.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderValidationException("amount must be positive");
        }
        if (event.currency() == null || !ALLOWED_CURRENCIES.contains(event.currency())) {
            throw new OrderValidationException("currency is not supported");
        }
        if (event.createdAt() == null) {
            throw new OrderValidationException("createdAt is required");
        }
    }
}