package com.example.kafkaorders.e2e;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import com.example.kafkaorders.support.AbstractIntegrationTest;
import com.example.kafkaorders.support.KafkaTestConsumer;
import com.example.kafkaorders.support.OrderEventFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDlqE2ETest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProcessedOrderRepository repository;

    private KafkaTestConsumer<OrderCreatedEvent> dlqConsumer;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (dlqConsumer != null) {
            dlqConsumer.close();
        }
    }

    @Test
    void shouldSendMessageToDlqAfterRetriesExhausted() throws Exception {
        OrderCreatedEvent event = OrderEventFactory.transientFailureOrder();

        dlqConsumer = new KafkaTestConsumer<>(
                kafka.getBootstrapServers(),
                "dlq-consumer-" + UUID.randomUUID(),
                OrderCreatedEvent.class,
                "orders.dlq"
        );

        kafkaTemplate.send("orders.created", event.orderId(), event)
                .get(5, TimeUnit.SECONDS);

        var dlqRecord = dlqConsumer.pollUntil(
                Duration.ofSeconds(20),
                record -> record.value() != null
                        && event.eventId().equals(record.value().eventId())
        );

        assertThat(dlqRecord).isNotNull();
        assertThat(dlqRecord.value().eventId()).isEqualTo(event.eventId());
        assertThat(repository.findByOrderId(event.orderId())).isEmpty();
    }
}