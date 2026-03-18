package com.example.kafkaorders.service;

import com.example.kafkaorders.dto.OrderFailedEvent;
import com.example.kafkaorders.dto.OrderProcessedEvent;
import com.example.kafkaorders.support.TopicNames;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TopicNames topicNames;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, TopicNames topicNames) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicNames = topicNames;
    }

    public void publishProcessed(OrderProcessedEvent event) {
        kafkaTemplate.send(topicNames.ordersProcessed(), event.orderId(), event);
    }

    public void publishFailed(OrderFailedEvent event) {
        kafkaTemplate.send(topicNames.ordersFailed(), event.orderId(), event);
    }
}
