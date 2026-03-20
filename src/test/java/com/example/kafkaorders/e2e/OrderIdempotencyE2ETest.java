package com.example.kafkaorders.e2e;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import com.example.kafkaorders.support.AbstractIntegrationTest;
import com.example.kafkaorders.support.OrderEventFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OrderIdempotencyE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProcessedOrderRepository repository;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @Test
    void shouldNotCreateDuplicateRecordForSameEventId() {
        String uniqueOrderId = "ORD-" + UUID.randomUUID();

        OrderCreatedEvent base = OrderEventFactory.validOrder();
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                uniqueOrderId,
                base.userId(),
                base.amount(),
                base.currency(),
                Instant.now()
        );

        OrderCreatedEvent duplicateEvent = new OrderCreatedEvent(
                event.eventId(),
                event.orderId(),
                event.userId(),
                event.amount(),
                event.currency(),
                event.createdAt()
        );

        kafkaTemplate.send("orders.created", event.orderId(), event);
        kafkaTemplate.send("orders.created", duplicateEvent.orderId(), duplicateEvent);

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    assertThat(repository.findAll()).hasSize(1);
                    assertThat(repository.findByOrderId(event.orderId())).isPresent();
                });

        var savedOrder = repository.findByOrderId(event.orderId()).orElseThrow();

        assertThat(savedOrder.getOrderId()).isEqualTo(event.orderId());
        assertThat(savedOrder.getEventId()).isEqualTo(event.eventId());
        assertThat(savedOrder.getUserId()).isEqualTo(event.userId());
        assertThat(savedOrder.getAmount()).isEqualTo(event.amount());
        assertThat(savedOrder.getCurrency()).isEqualTo(event.currency());
    }

    @Test
    void shouldNotCreateDuplicateRecordForSameOrderIdWithDifferentEventIds() {
        String uniqueOrderId = "ORD-" + UUID.randomUUID();

        OrderCreatedEvent base = OrderEventFactory.validOrder();
        OrderCreatedEvent firstEvent = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                uniqueOrderId,
                base.userId(),
                base.amount(),
                base.currency(),
                Instant.now()
        );

        OrderCreatedEvent secondEvent = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                uniqueOrderId,
                firstEvent.userId(),
                firstEvent.amount(),
                firstEvent.currency(),
                Instant.now()
        );

        kafkaTemplate.send("orders.created", firstEvent.orderId(), firstEvent);

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    assertThat(repository.findAll()).hasSize(1);
                    assertThat(repository.findByOrderId(firstEvent.orderId())).isPresent();
                });

        kafkaTemplate.send("orders.created", secondEvent.orderId(), secondEvent);

        Awaitility.await()
                .during(2, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    assertThat(repository.findAll()).hasSize(1);
                    assertThat(repository.findByOrderId(firstEvent.orderId())).isPresent();
                });

        var savedOrder = repository.findByOrderId(firstEvent.orderId()).orElseThrow();

        assertThat(savedOrder.getOrderId()).isEqualTo(firstEvent.orderId());
        assertThat(savedOrder.getEventId()).isEqualTo(firstEvent.eventId());
        assertThat(savedOrder.getUserId()).isEqualTo(firstEvent.userId());
        assertThat(savedOrder.getAmount()).isEqualTo(firstEvent.amount());
        assertThat(savedOrder.getCurrency()).isEqualTo(firstEvent.currency());
    }
}