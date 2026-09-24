package com.rpe.cardprocessor.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.rpe.cardprocessor.CardFixtures.card;
import static org.assertj.core.api.Assertions.assertThat;

class CardTest {

    @Test
    void newCardIsAtivoWithMaskedNumber() {
        Card card = card(UUID.randomUUID());

        assertThat(card.getStatus()).isEqualTo(CardStatus.ATIVO);
        assertThat(card.getMaskedNumber()).isEqualTo("**** **** **** 1234");
        assertThat(card.getOperator()).isEqualTo(CardOperator.MASTERCARD);
        assertThat(card.getProduct().name()).isEqualTo("GOLD");
    }

    @Test
    void toStringDoesNotExposeSensitiveData() {
        Card card = card(UUID.randomUUID());

        assertThat(card.toString()).doesNotContain("5112345678901234", "2031", "Maria");
    }
}
