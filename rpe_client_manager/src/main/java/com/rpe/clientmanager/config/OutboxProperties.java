package com.rpe.clientmanager.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** {@code app.outbox.*} from application.yml (the poll interval and purge cron are read by the @Scheduled methods). */
@Validated
@ConfigurationProperties("app.outbox")
public record OutboxProperties(
        @Min(1) int batchSize,
        @Min(1) int maxAttempts,
        @NotNull Duration initialBackoff,
        @NotNull Duration maxBackoff,
        @NotNull Duration retention,
        @NotBlank String encryptionKey
) {
}
