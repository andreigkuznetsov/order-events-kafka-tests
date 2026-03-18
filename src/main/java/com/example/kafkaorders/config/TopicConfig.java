package com.example.kafkaorders.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic ordersCreatedTopic(
            @Value("${app.kafka.topics.orders-created}") String topicName
    ) {
        return new NewTopic(topicName, 1, (short) 1);
    }

    @Bean
    public NewTopic ordersProcessedTopic(
            @Value("${app.kafka.topics.orders-processed}") String topicName
    ) {
        return new NewTopic(topicName, 1, (short) 1);
    }

    @Bean
    public NewTopic ordersFailedTopic(
            @Value("${app.kafka.topics.orders-failed}") String topicName
    ) {
        return new NewTopic(topicName, 1, (short) 1);
    }
}