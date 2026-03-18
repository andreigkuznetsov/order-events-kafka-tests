package com.example.kafkaorders.e2e;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.dto.OrderFailedEvent;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import com.example.kafkaorders.support.AbstractIntegrationTest;
import com.example.kafkaorders.support.KafkaTestConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UnsupportedCurrencyE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProcessedOrderRepository repository;

    private KafkaTestConsumer<OrderFailedEvent> failedConsumer;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (failedConsumer != null) {
            failedConsumer.close();
        }
    }

    @Test
    void shouldSendEventToFailedTopicWhenCurrencyIsUnsupported() throws ExecutionException, InterruptedException, TimeoutException {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "ORD-CUR-" + UUID.randomUUID(),
                "USER-CUR",
                new BigDecimal("100.00"),
                "ABC",
                Instant.now()
        );

        failedConsumer = new KafkaTestConsumer<>(
                kafka.getBootstrapServers(),
                "failed-currency-consumer-" + UUID.randomUUID(),
                OrderFailedEvent.class,
                "orders.failed"
        );

        kafkaTemplate.send("orders.created", event.orderId(), event)
                .get(5, java.util.concurrent.TimeUnit.SECONDS);

        var failedRecord = failedConsumer.pollUntil(
                Duration.ofSeconds(10),
                record -> record.value() != null
                        && event.eventId().equals(record.value().eventId())
                        && event.orderId().equals(record.value().orderId())
        );

        assertThat(failedRecord).isNotNull();
        assertThat(failedRecord.value().reason()).contains("currency");
        assertThat(repository.findByOrderId(event.orderId())).isEmpty();
    }
}