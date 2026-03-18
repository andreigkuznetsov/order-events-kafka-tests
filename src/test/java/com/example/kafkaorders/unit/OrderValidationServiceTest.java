package com.example.kafkaorders.unit;

import com.example.kafkaorders.service.OrderValidationService;
import com.example.kafkaorders.support.OrderEventFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderValidationServiceTest {

    private final OrderValidationService validationService = new OrderValidationService();

    @Test
    void shouldThrowWhenOrderIdIsMissing() {
        var event = OrderEventFactory.invalidWithoutOrderId();

        assertThatThrownBy(() -> validationService.validate(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderId is required");
    }
}
