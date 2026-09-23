package com.rpe.clientmanager.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.rpe.clientmanager.CustomerFixtures.customer;
import static org.assertj.core.api.Assertions.assertThat;

class CardProductionRequestedTest {

    @Test
    void toStringLeavesPersonalDataOut() {
        CardProductionRequested event = CardProductionRequested.of(customer(UUID.randomUUID()), "score=780", Instant.now());

        assertThat(event.toString())
                .contains(event.eventId().toString(), event.customerId().toString())
                .doesNotContain("Maria", "12345678909", "score=780");
    }
}
