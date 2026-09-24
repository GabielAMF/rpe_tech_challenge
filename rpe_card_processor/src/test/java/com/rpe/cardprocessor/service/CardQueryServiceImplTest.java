package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.domain.Card;
import com.rpe.cardprocessor.exception.CardNotFoundException;
import com.rpe.cardprocessor.exception.CatalogUnavailableException;
import com.rpe.cardprocessor.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.rpe.cardprocessor.CardFixtures.GOLD;
import static com.rpe.cardprocessor.CardFixtures.card;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardQueryServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CatalogGateway catalogGateway;

    @InjectMocks
    private CardQueryServiceImpl service;

    private final UUID customerId = UUID.randomUUID();

    @Test
    void returnsCardWithTheProductAsTheCatalogDescribesItNow() {
        Card card = card(customerId);
        CatalogProduct current = new CatalogProduct(GOLD.id(), "GOLD CLASSIC", "Renamed later", "CANCELADO");
        when(cardRepository.findByCustomerId(customerId)).thenReturn(Optional.of(card));
        when(catalogGateway.findProduct(GOLD.id())).thenReturn(Optional.of(current));

        CardDetails details = service.findByCustomerId(customerId);

        assertThat(details.card()).isSameAs(card);
        assertThat(details.product()).isEqualTo(current);
    }

    @Test
    void fallsBackToTheSnapshotWhenCatalogIsUnavailable() {
        when(cardRepository.findByCustomerId(customerId)).thenReturn(Optional.of(card(customerId)));
        when(catalogGateway.findProduct(GOLD.id())).thenThrow(new CatalogUnavailableException(new RuntimeException("down")));

        assertThat(service.findByCustomerId(customerId).product())
                .isEqualTo(new CatalogProduct(GOLD.id(), GOLD.name(), GOLD.description(), null));
    }

    @Test
    void fallsBackToTheSnapshotWhenProductIsGoneFromCatalog() {
        when(cardRepository.findByCustomerId(customerId)).thenReturn(Optional.of(card(customerId)));
        when(catalogGateway.findProduct(GOLD.id())).thenReturn(Optional.empty());

        assertThat(service.findByCustomerId(customerId).product())
                .isEqualTo(new CatalogProduct(GOLD.id(), GOLD.name(), GOLD.description(), null));
    }

    @Test
    void throwsWhenCustomerHasNoCard() {
        when(cardRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCustomerId(customerId)).isInstanceOf(CardNotFoundException.class);
        verifyNoInteractions(catalogGateway);
    }
}
