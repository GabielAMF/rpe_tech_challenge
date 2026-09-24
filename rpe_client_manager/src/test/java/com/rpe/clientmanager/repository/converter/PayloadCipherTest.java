package com.rpe.clientmanager.repository.converter;

import com.rpe.clientmanager.config.OutboxProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayloadCipherTest {

    private static final String KEY = "QmVp3Z1q7j5V0kqKp2XN4r8sYwT6uH9aLcE1dF3gJ0M=";

    private final PayloadCipher cipher = new PayloadCipher(properties(KEY));

    @Test
    void roundTripsWithoutLeakingThePlaintext() {
        String json = "{\"cpf\":\"12345678909\"}";

        String encrypted = cipher.encrypt(json);

        assertThat(encrypted).doesNotContain("12345678909");
        assertThat(cipher.decrypt(encrypted)).isEqualTo(json);
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        assertThat(cipher.encrypt("x")).isNotEqualTo(cipher.encrypt("x"));
    }

    @Test
    void tamperedValueFailsToDecrypt() {
        String encrypted = cipher.encrypt("{\"cpf\":\"12345678909\"}");
        String tampered = encrypted.substring(0, 20) + (encrypted.charAt(20) == 'A' ? 'B' : 'A') + encrypted.substring(21);

        assertThatThrownBy(() -> cipher.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("12345678909");
    }

    @Test
    void rejectsKeysThatAreNot256Bits() {
        assertThatThrownBy(() -> new PayloadCipher(properties("c2hvcnQ=")))
                .isInstanceOf(IllegalStateException.class);
    }

    private static OutboxProperties properties(String key) {
        return new OutboxProperties(50, 3, Duration.ofSeconds(2), Duration.ofMinutes(5), Duration.ofDays(7), key);
    }
}
