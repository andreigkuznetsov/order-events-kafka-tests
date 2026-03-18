package com.example.kafkaorders.unit;

import com.example.kafkaorders.dto.OrderFailedEvent;
import com.example.kafkaorders.dto.OrderProcessedEvent;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import com.example.kafkaorders.service.OrderEventPublisher;
import com.example.kafkaorders.service.OrderProcessingService;
import com.example.kafkaorders.service.OrderValidationService;
import com.example.kafkaorders.support.OrderEventFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private ProcessedOrderRepository repository;

    @Mock
    private OrderEventPublisher publisher;

    private final OrderValidationService validationService = new OrderValidationService();

    @Test
    void shouldPublishProcessedEventForValidOrder() {
        var service = new OrderProcessingService(validationService, repository, publisher);
        var event = OrderEventFactory.validOrder();

        when(repository.existsByEventId(event.eventId())).thenReturn(false);

        service.process(event);

        var eventCaptor = ArgumentCaptor.forClass(OrderProcessedEvent.class);
        verify(publisher).publishProcessed(eventCaptor.capture());
        verify(publisher, never()).publishFailed(any(OrderFailedEvent.class));
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(event.orderId());
        assertThat(eventCaptor.getValue().status()).isEqualTo("PROCESSED");
    }

    @Test
    void shouldPublishFailedEventForInvalidOrder() {
        var service = new OrderProcessingService(validationService, repository, publisher);
        var event = OrderEventFactory.invalidWithoutOrderId();

        service.process(event);

        var eventCaptor = ArgumentCaptor.forClass(OrderFailedEvent.class);
        verify(publisher).publishFailed(eventCaptor.capture());
        verify(publisher, never()).publishProcessed(any(OrderProcessedEvent.class));
        assertThat(eventCaptor.getValue().reason()).contains("orderId is required");
    }
}
