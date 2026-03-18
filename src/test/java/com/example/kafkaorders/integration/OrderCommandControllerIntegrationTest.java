package com.example.kafkaorders.integration;

import com.example.kafkaorders.dto.CreateOrderRequest;
import com.example.kafkaorders.dto.OrderProcessedEvent;
import com.example.kafkaorders.repository.ProcessedOrderRepository;
import com.example.kafkaorders.support.AbstractIntegrationTest;
import com.example.kafkaorders.support.KafkaTestConsumer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCommandControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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
    void shouldAcceptOrderViaHttpAndProcessItEndToEnd() {
        String orderId = "ORD-REST-" + UUID.randomUUID();

        processedConsumer = new KafkaTestConsumer<>(
                kafka.getBootstrapServers(),
                "processed-rest-consumer-" + UUID.randomUUID(),
                OrderProcessedEvent.class,
                "orders.processed"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateOrderRequest requestBody = new CreateOrderRequest(
                orderId,
                "USER-REST",
                new BigDecimal("999.99"),
                "RUB"
        );

        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    var savedOrder = repository.findByOrderId(orderId);
                    assertThat(savedOrder).isPresent();
                    assertThat(savedOrder.get().getOrderId()).isEqualTo(orderId);
                    assertThat(savedOrder.get().getUserId()).isEqualTo("USER-REST");
                    assertThat(savedOrder.get().getAmount()).isEqualByComparingTo(new BigDecimal("999.99"));
                    assertThat(savedOrder.get().getCurrency()).isEqualTo("RUB");
                    assertThat(savedOrder.get().getProcessedAt()).isNotNull();
                });

        var processedRecord = processedConsumer.pollUntil(
                Duration.ofSeconds(10),
                record -> record.value() != null
                        && orderId.equals(record.value().orderId())
        );

        assertThat(processedRecord).isNotNull();
        assertThat(processedRecord.value().userId()).isEqualTo("USER-REST");
        assertThat(processedRecord.value().amount()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(processedRecord.value().currency()).isEqualTo("RUB");
        assertThat(processedRecord.value().status()).isEqualTo("PROCESSED");
    }

    @Test
    void shouldReturnBadRequestForInvalidOrderRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                "",
                "",
                BigDecimal.ZERO,
                ""
        );

        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(invalidRequest, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Validation failed");
        assertThat(response.getBody()).contains("orderId");
        assertThat(response.getBody()).contains("userId");
        assertThat(response.getBody()).contains("amount");
        assertThat(response.getBody()).contains("currency");

        assertThat(repository.findAll()).isEmpty();
    }
}