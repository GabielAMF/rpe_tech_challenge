package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.domain.Card;
import com.rpe.cardprocessor.domain.CardProduct;
import com.rpe.cardprocessor.exception.CardNotFoundException;
import com.rpe.cardprocessor.exception.CatalogUnavailableException;
import com.rpe.cardprocessor.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Not transactional on purpose: the catalog lookup is an HTTP call and must not hold a database connection.
 * The product comes from rpe_catalog (cached), so renames and status changes show up; a catalog outage never hides
 * the card, it falls back to the snapshot stored with it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardQueryServiceImpl implements CardQueryService {

    private final CardRepository cardRepository;
    private final CatalogGateway catalogGateway;

    @Override
    public CardDetails findByCustomerId(UUID customerId) {
        Card card = cardRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CardNotFoundException(customerId));
        return new CardDetails(card, currentProduct(card.getProduct()));
    }

    private CatalogProduct currentProduct(CardProduct snapshot) {
        try {
            return catalogGateway.findProduct(snapshot.id()).orElseGet(() -> {
                log.warn("Product id={} of an issued card is no longer in rpe_catalog; using the card's snapshot",
                        snapshot.id());
                return fromSnapshot(snapshot);
            });
        } catch (CatalogUnavailableException ex) {
            log.warn("Product id={} unavailable, using the card's snapshot: {}", snapshot.id(), ex.getCause().toString());
            return fromSnapshot(snapshot);
        }
    }

    /** Status unknown: the snapshot only says what the product was at issue time. */
    private static CatalogProduct fromSnapshot(CardProduct snapshot) {
        return new CatalogProduct(snapshot.id(), snapshot.name(), snapshot.description(), null);
    }
}
