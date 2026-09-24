package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.domain.Card;

/** Card production use case, triggered by rpe_client_manager's card production requests. */
public interface CardProductionService {

    /**
     * Produces the customer's card, or returns the existing one if this event (or this customer) was already
     * handled: messages are delivered at least once, so the same request may arrive twice.
     *
     * @throws com.rpe.cardprocessor.exception.CatalogUnavailableException if rpe_catalog can't be reached
     */
    Card produce(CardProductionCommand command);
}
