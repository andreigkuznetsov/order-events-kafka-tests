package com.example.kafkaorders.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(

        @NotBlank(message = "orderId must not be blank")
        String orderId,

        @NotBlank(message = "userId must not be blank")
        String userId,

        @NotNull(message = "amount must not be null")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "currency must not be blank")
        String currency
) {
}