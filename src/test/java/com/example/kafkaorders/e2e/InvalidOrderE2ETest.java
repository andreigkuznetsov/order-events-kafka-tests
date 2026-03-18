package com.example.kafkaorders.e2e;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.dto.OrderFailedEvent;
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
class InvalidOrderE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProcessedOrderRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSendInvalidOrderToFailedTopicAndNotSaveToDb() {
        OrderCreatedEvent event = OrderEventFactory.invalidWithoutOrderId();

        try (KafkaTestConsumer<OrderFailedEvent> failedConsumer = new KafkaTestConsumer<>(
                kafka.getBootstrapServers(),
                "failed-consumer-" + UUID.randomUUID(),
                OrderFailedEvent.class,
                "orders.failed"
        )) {
            kafkaTemplate.send("orders.created", "missing-order-id", event);

            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> {
                        var record = failedConsumer.pollSingleRecord(Duration.ofMillis(500));
                        assertThat(record).isNotNull();
                        assertThat(record.value().reason()).contains("orderId is required");
                    });

            assertThat(repository.findAll()).isEmpty();
        }
    }
}
