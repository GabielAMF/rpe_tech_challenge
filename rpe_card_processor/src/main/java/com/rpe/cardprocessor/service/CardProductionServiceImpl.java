package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.domain.Card;
import com.rpe.cardprocessor.domain.CardProduct;
import com.rpe.cardprocessor.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Logs carry ids, operator and product only: never the holder name, number, expiry, CVV or credit info. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardProductionServiceImpl implements CardProductionService {

    private final CardRepository cardRepository;
    private final ProductSelectionPolicy productSelectionPolicy;
    private final CardDataGenerator cardDataGenerator;

    @Override
    @Transactional
    public Card produce(CardProductionCommand command) {
        Optional<Card> existing = cardRepository.findByCustomerId(command.customerId());
        if (existing.isPresent()) {
            log.info("Card for customer id={} already exists (card id={}); ignoring {}",
                    command.customerId(), existing.get().getId(), command);
            return existing.get();
        }

        CatalogProduct product = productSelectionPolicy.select(command.creditInfo());
        Card card = cardRepository.saveAndFlush(new Card(
                command.eventId(),
                command.customerId(),
                command.holderName(),
                cardDataGenerator.generate(),
                new CardProduct(product.id(), product.name(), product.description())));
        log.info("Produced card id={} for customer id={} operator={} product={}",
                card.getId(), card.getCustomerId(), card.getOperator(), product.name());
        return card;
    }
}
