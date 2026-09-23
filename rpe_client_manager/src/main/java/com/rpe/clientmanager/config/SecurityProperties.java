package com.rpe.clientmanager.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** {@code app.security.*} from application.yml. Validated at startup so a weak JWT secret fails fast. */
@Validated
@ConfigurationProperties("app.security")
public record SecurityProperties(@Valid @NotNull Jwt jwt, @Valid BootstrapUser bootstrapUser) {

    /** HS256 needs a key of at least 256 bits, hence the 32-character minimum. */
    public record Jwt(@NotBlank @Size(min = 32) String secret, @NotNull Duration expiration, @NotBlank String issuer) {
    }

    public record BootstrapUser(String username, String password) {
    }
}
