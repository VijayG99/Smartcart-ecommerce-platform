package com.smartcart.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Consumer error handling: retry a failed record 3 times (1 second apart),
 * then publish it to "order-events.DLT" and move on. Spring Boot applies this bean to all @KafkaListener containers.
 */
@Configuration
public class KafkaErrorConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
}
