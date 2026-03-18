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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    void shouldConsumeOrderSaveToDbAndPublishProcessedEvent() {
        OrderCreatedEvent event = OrderEventFactory.validOrder();

        try (KafkaTestConsumer<OrderProcessedEvent> processedConsumer = new KafkaTestConsumer<>(
                kafka.getBootstrapServers(),
                "processed-consumer-" + UUID.randomUUID(),
                OrderProcessedEvent.class,
                "orders.processed"
        )) {
            kafkaTemplate.send("orders.created", event.orderId(), event);

            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> {
                        var savedOrder = repository.findByOrderId(event.orderId());
                        assertThat(savedOrder).isPresent();
                        assertThat(savedOrder.get().getUserId()).isEqualTo(event.userId());
                        assertThat(savedOrder.get().getAmount()).isEqualByComparingTo(event.amount());
                    });

            OrderProcessedEvent processedEvent = null;
            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> {
                        var record = processedConsumer.pollSingleRecord(Duration.ofMillis(500));
                        assertThat(record).isNotNull();
                        assertThat(record.value().orderId()).isEqualTo(event.orderId());
                        assertThat(record.value().status()).isEqualTo("PROCESSED");
                    });
        }
    }
}
