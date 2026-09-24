package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.exception.InvalidCardProductionRequestException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardProductionCommandTest {

    private final UUID eventId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @Test
    void requiresEventIdCustomerIdAndHolderName() {
        assertThatThrownBy(() -> new CardProductionCommand(null, customerId, "Maria", null))
                .isInstanceOf(InvalidCardProductionRequestException.class).hasMessageContaining("eventId");
        assertThatThrownBy(() -> new CardProductionCommand(eventId, null, "Maria", null))
                .isInstanceOf(InvalidCardProductionRequestException.class).hasMessageContaining("customerId");
        assertThatThrownBy(() -> new CardProductionCommand(eventId, customerId, " ", null))
                .isInstanceOf(InvalidCardProductionRequestException.class).hasMessageContaining("customerName");
    }

    @Test
    void creditInfoIsOptionalAndHiddenFromToString() {
        CardProductionCommand command = new CardProductionCommand(eventId, customerId, "Maria Silva", "score=780");

        assertThat(new CardProductionCommand(eventId, customerId, "Maria Silva", null).creditInfo()).isNull();
        assertThat(command.toString()).doesNotContain("Maria").doesNotContain("score");
    }
}
