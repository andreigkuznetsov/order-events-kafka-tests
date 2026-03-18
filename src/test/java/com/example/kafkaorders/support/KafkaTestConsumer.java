package com.example.kafkaorders.support;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.time.Instant;

public class KafkaTestConsumer<T> implements AutoCloseable {

    private final Consumer<String, T> consumer;

    public KafkaTestConsumer(String bootstrapServers, String groupId, Class<T> clazz, String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, clazz.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        this.consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(topic));
    }

    public ConsumerRecord<String, T> pollSingleRecord(Duration timeout) {
        var records = consumer.poll(timeout);
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next();
    }

    public ConsumerRecord<String, T> pollUntil(Duration timeout,
                                               java.util.function.Predicate<ConsumerRecord<String, T>> predicate) {

        Instant deadline = Instant.now().plus(timeout);

        while (Instant.now().isBefore(deadline)) {
            var records = consumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, T> record : records) {
                if (predicate.test(record)) {
                    return record;
                }
            }
        }

        return null;
    }

    public void close() {
        consumer.close();
    }
}
