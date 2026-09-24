package com.rpe.clientmanager.config;

import com.rpe.clientmanager.domain.OutboxRetryPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduling for OutboxRelay and OutboxPurger. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfig {

    @Bean
    OutboxRetryPolicy outboxRetryPolicy(OutboxProperties properties) {
        return new OutboxRetryPolicy(properties.maxAttempts(), properties.initialBackoff(), properties.maxBackoff());
    }
}
