package com.rpe.cardprocessor.config;

import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ExponentialBackoffErrorHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Retry strategy for consumed messages. When the listener throws, the message is not deleted; this error handler
 * sets its visibility timeout from its receive count (exponential backoff), so SQS redelivers it later instead of
 * after the queue's fixed visibility timeout. After {@code maxReceiveCount} receives (3, the queue's redrive policy,
 * see docker/localstack/init) SQS moves it to the DLQ. Picked up by Spring Cloud AWS's default listener factory.
 * <p>
 * Scheduling is for DeadLetterQueueMonitor.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SqsRetryProperties.class)
public class SqsConfig {

    @Bean
    AsyncErrorHandler<Object> exponentialBackoffErrorHandler(SqsRetryProperties retry) {
        return ExponentialBackoffErrorHandler.builder()
                .initialVisibilityTimeoutSeconds((int) retry.initialBackoff().toSeconds())
                .multiplier(retry.multiplier())
                .maxVisibilityTimeoutSeconds((int) retry.maxBackoff().toSeconds())
                .build();
    }
}
