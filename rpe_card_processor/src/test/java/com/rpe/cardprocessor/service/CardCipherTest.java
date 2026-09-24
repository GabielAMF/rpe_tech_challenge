package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.TestCardProperties;
import com.rpe.cardprocessor.config.CardProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardCipherTest {

    private final CardCipher cipher = new CardCipher(TestCardProperties.properties());

    @Test
    void roundTrips() {
        assertThat(cipher.decrypt(cipher.encrypt("5112345678901234"))).isEqualTo("5112345678901234");
    }

    @Test
    void ciphertextHidesThePlaintextAndDiffersEachTime() {
        String first = cipher.encrypt("5112345678901234");
        String second = cipher.encrypt("5112345678901234");

        assertThat(first).doesNotContain("5112345678901234").isNotEqualTo(second);
    }

    @Test
    void tamperedValueFailsWithoutEchoingIt() {
        byte[] data = Base64.getDecoder().decode(cipher.encrypt("123"));
        data[data.length - 1] ^= 1;

        assertThatThrownBy(() -> cipher.decrypt(Base64.getEncoder().encodeToString(data)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("123");
    }

    @Test
    void anotherKeyCantDecrypt() {
        String encrypted = cipher.encrypt("123");
        CardCipher other = new CardCipher(new CardProperties(TestCardProperties.DEFAULT_PRODUCT_ID,
                Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32}), 5));

        assertThatThrownBy(() -> other.decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsKeyThatIsNot256Bits() {
        assertThatThrownBy(() -> new CardCipher(new CardProperties(TestCardProperties.DEFAULT_PRODUCT_ID,
                Base64.getEncoder().encodeToString(new byte[16]), 5)))
                .isInstanceOf(IllegalStateException.class);
    }
}
