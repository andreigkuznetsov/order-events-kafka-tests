package com.example.kafkaorders.e2e;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.dto.OrderProcessedEvent;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import com.example.kafkaorders.support.AbstractIntegrationTest;
import com.example.kafkaorders.support.KafkaTestConsumer;
import com.example.kafkaorders.support.OrderEventFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OrderIdempotencyE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProcessedOrderRepository repository;

    private KafkaTestConsumer<OrderProcessedEvent> processedConsumer;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (processedConsumer != null) {
            processedConsumer.close();
        }
    }

    @Test
    void shouldNotCreateDuplicateRecordForSameEventId() {
        OrderCreatedEvent event = OrderEventFactory.validOrder();

        OrderCreatedEvent duplicateEvent = new OrderCreatedEvent(
                event.eventId(),
                event.orderId(),
                event.userId(),
                event.amount(),
                event.currency(),
                event.createdAt()
        );

        processedConsumer = new KafkaTestConsumer<>(
                kafka.getBootstrapServers(),
                "processed-idempotency-consumer-" + UUID.randomUUID(),
                OrderProcessedEvent.class,
                "orders.processed"
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

        List<OrderProcessedEvent> processedEvents = new ArrayList<>();

        var firstRecord = processedConsumer.pollSingleRecord(Duration.ofSeconds(5));
        if (firstRecord != null) {
            processedEvents.add(firstRecord.value());
        }

        var secondRecord = processedConsumer.pollSingleRecord(Duration.ofSeconds(2));
        if (secondRecord != null) {
            processedEvents.add(secondRecord.value());
        }

        assertThat(processedEvents).hasSize(1);
        assertThat(processedEvents.getFirst().eventId()).isEqualTo(event.eventId());
        assertThat(processedEvents.getFirst().orderId()).isEqualTo(event.orderId());
    }
}