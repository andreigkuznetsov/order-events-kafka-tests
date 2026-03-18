package com.example.kafkaorders.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "processed_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processed_orders_order_id", columnNames = "order_id")
})
public class ProcessedOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedOrderEntity() {
    }

    public ProcessedOrderEntity(
            String eventId,
            String orderId,
            String userId,
            BigDecimal amount,
            String currency,
            Instant processedAt
    ) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.processedAt = processedAt;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getProcessedAt() { return processedAt; }
}