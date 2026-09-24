package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.domain.Card;
import com.rpe.cardprocessor.exception.CardNotFoundException;
import com.rpe.cardprocessor.exception.CatalogUnavailableException;
import com.rpe.cardprocessor.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Not transactional on purpose: the catalog lookup is an HTTP call and must not hold a database connection.
 * The product status is best effort, so a catalog outage never hides the card.
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
        return new CardDetails(card, currentProductStatus(card.getProduct().id()));
    }

    private String currentProductStatus(UUID productId) {
        try {
            return catalogGateway.findProduct(productId)
                    .map(CatalogProduct::status)
                    .orElseGet(() -> {
                        log.warn("Product id={} of an issued card is no longer in rpe_catalog", productId);
                        return null;
                    });
        } catch (CatalogUnavailableException ex) {
            log.warn("Status of product id={} unavailable: {}", productId, ex.getCause().toString());
            return null;
        }
    }
}
