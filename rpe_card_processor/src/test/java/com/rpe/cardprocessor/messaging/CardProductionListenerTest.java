package com.rpe.cardprocessor.messaging;

import com.rpe.cardprocessor.exception.InvalidCardProductionRequestException;
import com.rpe.cardprocessor.service.CardProductionCommand;
import com.rpe.cardprocessor.service.CardProductionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CardProductionListenerTest {

    private final UUID eventId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @Mock
    private CardProductionService cardProductionService;

    @InjectMocks
    private CardProductionListener listener;

    @Test
    void producesCardFromMessage() {
        listener.onMessage(message("CARD_PRODUCTION_REQUESTED", customerId, "Maria Silva"));

        verify(cardProductionService).produce(new CardProductionCommand(eventId, customerId, "Maria Silva", "score=780"));
    }

    @Test
    void ignoresUnknownEventTypes() {
        listener.onMessage(message("SOMETHING_ELSE", customerId, "Maria Silva"));

        verifyNoInteractions(cardProductionService);
    }

    @Test
    void rejectsMessageWithoutCustomer() {
        assertThatThrownBy(() -> listener.onMessage(message("CARD_PRODUCTION_REQUESTED", null, "Maria Silva")))
                .isInstanceOf(InvalidCardProductionRequestException.class);
        verifyNoInteractions(cardProductionService);
    }

    @Test
    void rejectsMessageWithoutName() {
        assertThatThrownBy(() -> listener.onMessage(message("CARD_PRODUCTION_REQUESTED", customerId, " ")))
                .isInstanceOf(InvalidCardProductionRequestException.class);
    }

    @Test
    void messageToStringHidesPersonalData() {
        assertThat(message("CARD_PRODUCTION_REQUESTED", customerId, "Maria Silva").toString())
                .doesNotContain("Maria", "12345678909", "score");
    }

    private CardProductionRequestedMessage message(String type, UUID customer, String name) {
        return new CardProductionRequestedMessage(eventId, type, Instant.now(), customer, name, "12345678909", "score=780");
    }
}
