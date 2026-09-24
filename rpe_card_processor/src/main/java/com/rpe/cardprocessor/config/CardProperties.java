package com.rpe.cardprocessor.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/** {@code app.card.*} from application.yml. */
@Validated
@ConfigurationProperties("app.card")
public record CardProperties(
        @NotNull UUID defaultProductId,
        @NotBlank String encryptionKey,
        @Min(1) int validityYears
) {
}
