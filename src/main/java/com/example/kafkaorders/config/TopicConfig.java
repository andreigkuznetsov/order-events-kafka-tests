package com.example.kafkaorders.config;

import com.example.kafkaorders.support.TopicNames;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic ordersCreatedTopic(TopicNames topicNames) {
        return new NewTopic(topicNames.ordersCreated(), 1, (short) 1);
    }

    @Bean
    public NewTopic ordersProcessedTopic(TopicNames topicNames) {
        return new NewTopic(topicNames.ordersProcessed(), 1, (short) 1);
    }

    @Bean
    public NewTopic ordersFailedTopic(TopicNames topicNames) {
        return new NewTopic(topicNames.ordersFailed(), 1, (short) 1);
    }

    @Bean
    public NewTopic ordersDlqTopic(TopicNames topicNames) {
        return new NewTopic(topicNames.ordersDlq(), 1, (short) 1);
    }
}