package com.example.kafkaorders.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TopicNames {

    @Value("${app.kafka.topics.orders-created}")
    private String ordersCreated;

    @Value("${app.kafka.topics.orders-processed}")
    private String ordersProcessed;

    @Value("${app.kafka.topics.orders-failed}")
    private String ordersFailed;

    public String ordersCreated() {
        return ordersCreated;
    }
    public String ordersProcessed() {
        return ordersProcessed;
    }
    public String ordersFailed() {
        return ordersFailed;
    }
}