package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.domain.Card;
import com.rpe.cardprocessor.domain.CardStatus;
import com.rpe.cardprocessor.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.rpe.cardprocessor.CardFixtures.DATA;
import static com.rpe.cardprocessor.CardFixtures.GOLD;
import static com.rpe.cardprocessor.CardFixtures.card;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardProductionServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ProductSelectionPolicy productSelectionPolicy;

    @Mock
    private CardDataGenerator cardDataGenerator;

    @InjectMocks
    private CardProductionServiceImpl service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @Test
    void producesCardForSelectedProduct() {
        when(cardRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(productSelectionPolicy.select("score=780")).thenReturn(GOLD);
        when(cardDataGenerator.generate()).thenReturn(DATA);
        when(cardRepository.saveAndFlush(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card card = service.produce(new CardProductionCommand(eventId, customerId, "Maria Silva", "score=780"));

        assertThat(card.getSourceEventId()).isEqualTo(eventId);
        assertThat(card.getCustomerId()).isEqualTo(customerId);
        assertThat(card.getHolderName()).isEqualTo("Maria Silva");
        assertThat(card.getNumber()).isEqualTo(DATA.number());
        assertThat(card.getStatus()).isEqualTo(CardStatus.ATIVO);
        assertThat(card.getProduct().id()).isEqualTo(GOLD.id());
    }

    @Test
    void secondRequestForSameCustomerReturnsExistingCard() {
        Card existing = card(customerId);
        when(cardRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existing));

        Card card = service.produce(new CardProductionCommand(eventId, customerId, "Maria Silva", "score=780"));

        assertThat(card).isSameAs(existing);
        verifyNoInteractions(productSelectionPolicy, cardDataGenerator);
        verify(cardRepository, never()).saveAndFlush(any());
    }

    @Test
    void commandToStringHidesPersonalData() {
        assertThat(new CardProductionCommand(eventId, customerId, "Maria Silva", "score=780").toString())
                .doesNotContain("Maria", "score");
    }
}
