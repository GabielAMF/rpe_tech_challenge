package com.rpe.cardprocessor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

@Configuration
@EnableConfigurationProperties(CardProperties.class)
public class CardConfig {

    /** Card numbers, CVVs and operators are drawn from a cryptographically strong source. */
    @Bean
    RandomGenerator cardRandom() {
        return new SecureRandom();
    }
}
