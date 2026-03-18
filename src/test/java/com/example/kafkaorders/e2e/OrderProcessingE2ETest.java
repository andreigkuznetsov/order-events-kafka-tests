package com.example.kafkaorders.e2e;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.dto.OrderProcessedEvent;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import com.example.kafkaorders.support.AbstractIntegrationTest;
import com.example.kafkaorders.support.KafkaTestConsumer;
import com.example.kafkaorders.support.OrderEventFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OrderProcessingE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProcessedOrderRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void shouldConsumeOrderSaveToDbAndPublishProcessedEvent() throws Exception {
        OrderCreatedEvent event = OrderEventFactory.validOrder();

        try (KafkaTestConsumer<OrderProcessedEvent> processedConsumer = new KafkaTestConsumer<>(
                kafka.getBootstrapServers(),
                "processed-consumer-" + UUID.randomUUID(),
                OrderProcessedEvent.class,
                "orders.processed"
        )) {
            kafkaTemplate.send("orders.created", event.orderId(), event)
                    .get(5, TimeUnit.SECONDS);

            Awaitility.await()
                    .atMost(15, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> {
                        var savedOrder = repository.findByOrderId(event.orderId());
                        assertThat(savedOrder).isPresent();
                        assertThat(savedOrder.get().getEventId()).isEqualTo(event.eventId());
                        assertThat(savedOrder.get().getOrderId()).isEqualTo(event.orderId());
                        assertThat(savedOrder.get().getUserId()).isEqualTo(event.userId());
                        assertThat(savedOrder.get().getAmount()).isEqualByComparingTo(event.amount());
                        assertThat(savedOrder.get().getCurrency()).isEqualTo(event.currency());
                        assertThat(savedOrder.get().getProcessedAt()).isNotNull();
                    });

            var record = processedConsumer.pollUntil(
                    Duration.ofSeconds(10),
                    r -> r.value() != null && event.orderId().equals(r.value().orderId())
            );

            assertThat(record).isNotNull();
            assertThat(record.value().orderId()).isEqualTo(event.orderId());
            assertThat(record.value().status()).isEqualTo("PROCESSED");
        }
    }
}