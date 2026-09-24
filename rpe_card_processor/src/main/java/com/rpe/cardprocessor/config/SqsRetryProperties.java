package com.rpe.cardprocessor.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * {@code app.sqs.retry.*}: how long a failed message stays invisible before SQS delivers it again,
 * {@code initialBackoff * multiplier^(receiveCount-1)}, capped at {@code maxBackoff}.
 */
@Validated
@ConfigurationProperties("app.sqs.retry")
public record SqsRetryProperties(
        @NotNull Duration initialBackoff,
        @DecimalMin("1.0") double multiplier,
        @NotNull Duration maxBackoff
) {
}
