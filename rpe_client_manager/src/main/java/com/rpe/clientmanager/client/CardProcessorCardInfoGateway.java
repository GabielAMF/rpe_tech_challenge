package com.rpe.clientmanager.client;

import com.rpe.clientmanager.client.dto.CardProcessorCardResponse;
import com.rpe.clientmanager.service.CardInfo;
import com.rpe.clientmanager.service.CardInfoGateway;
import com.rpe.clientmanager.service.CardLookup;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Card lookups degrade instead of failing: a customer can always be read, even with rpe_card_processor down.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardProcessorCardInfoGateway implements CardInfoGateway {

    private final CardProcessorClient client;

    @Override
    public CardLookup findByCustomerId(UUID customerId) {
        try {
            return CardLookup.found(toCardInfo(client.findCardByCustomerId(customerId)));
        } catch (FeignException.NotFound ex) {
            return CardLookup.notProducedYet();
        } catch (RuntimeException ex) {
            // Connection refused, timeout (RetryableException), 5xx, unreadable body...
            log.warn("Card info for customer id={} unavailable: {}", customerId, ex.toString());
            return CardLookup.unavailable();
        }
    }

    private static CardInfo toCardInfo(CardProcessorCardResponse response) {
        CardProcessorCardResponse.Product product = response.product();
        return new CardInfo(
                response.cardId(),
                response.status(),
                response.maskedNumber(),
                product == null ? null
                        : new CardInfo.Product(product.id(), product.name(), product.description(), product.status()),
                response.createdAt());
    }
}
