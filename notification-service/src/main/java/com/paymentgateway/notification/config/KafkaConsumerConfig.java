package com.paymentgateway.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        // Dead Letter Topic publisher sending failed records to <topic>.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, ex) -> {
                    log.error("Kafka consumer error: Exhausted all retries for record with key={} on topic={}. Routing to DLT.",
                            record.key(), record.topic(), ex);
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                }
        );

        // Exponential backoff: 3 retries (1000ms initial, 2.0x multiplier, max 5000ms interval)
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(5000L);
        backOff.setMaxElapsedTime(15000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        // Non-retryable exceptions can be added here if needed
        return errorHandler;
    }
}
