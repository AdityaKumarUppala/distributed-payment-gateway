package com.paymentgateway.audit.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, ex) -> {
                    log.error("Kafka audit consumer error: Exhausted all retries for record with key={} on topic={}. Routing to DLT.",
                            record.key(), record.topic(), ex);
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                }
        );

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(5000L);
        backOff.setMaxElapsedTime(15000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
