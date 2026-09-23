package com.rpe.clientmanager.service;

/**
 * Sends card production requests to rpe_card_processor. The service depends on this interface, not on SQS.
 */
public interface CardProductionPublisher {

    /** @throws com.rpe.clientmanager.exception.CardProductionUnavailableException if the request can't be sent */
    void publish(CardProductionRequested event);
}
