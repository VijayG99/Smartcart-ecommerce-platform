package com.smartcart.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/** Topics are created on startup by Spring's KafkaAdmin. Skipped in the "test" profile (no broker there). */
@Configuration
@Profile("!test")
public class KafkaTopicConfig {

    @Bean
    NewTopic orderEventsTopic(@Value("${smartcart.kafka.order-topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    /** Dead-letter topic: events that still fail after retries are parked here instead of blocking the partition. */
    @Bean
    NewTopic orderEventsDeadLetterTopic(@Value("${smartcart.kafka.order-topic}") String topic) {
        return TopicBuilder.name(topic + ".DLT").partitions(3).replicas(1).build();
    }
}
