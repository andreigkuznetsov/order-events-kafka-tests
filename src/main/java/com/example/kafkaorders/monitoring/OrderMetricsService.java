package com.example.kafkaorders.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OrderMetricsService {

    private final Counter createdCounter;
    private final Counter processedCounter;
    private final Counter failedCounter;
    private final Counter dlqCounter;
    private final Timer processingTimer;

    public OrderMetricsService(MeterRegistry meterRegistry) {
        this.createdCounter = Counter.builder("orders_created_total")
                .description("Total number of created order events")
                .register(meterRegistry);

        this.processedCounter = Counter.builder("orders_processed_total")
                .description("Total number of successfully processed order events")
                .register(meterRegistry);

        this.failedCounter = Counter.builder("orders_failed_total")
                .description("Total number of failed order events")
                .register(meterRegistry);

        this.dlqCounter = Counter.builder("orders_dlq_total")
                .description("Total number of order events sent to DLQ")
                .register(meterRegistry);

        this.processingTimer = Timer.builder("orders_processing_duration_ms")
                .description("Order processing duration")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    public void recordCreated() {
        createdCounter.increment();
    }

    public void recordProcessed() {
        processedCounter.increment();
    }

    public void recordFailed() {
        failedCounter.increment();
    }

    public void recordDlq() {
        dlqCounter.increment();
    }

    public void recordProcessingTime(long durationMillis) {
        processingTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }
}