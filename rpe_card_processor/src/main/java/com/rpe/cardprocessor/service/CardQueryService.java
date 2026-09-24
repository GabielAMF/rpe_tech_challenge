package com.rpe.cardprocessor.service;

import java.util.UUID;

/** Read side of cards, used by rpe_client_manager to show a customer's card. */
public interface CardQueryService {

    /**
     * @throws com.rpe.cardprocessor.exception.CardNotFoundException if the customer has no card (yet)
     */
    CardDetails findByCustomerId(UUID customerId);
}
