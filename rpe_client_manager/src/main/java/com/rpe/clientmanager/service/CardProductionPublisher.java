package com.rpe.clientmanager.service;

/**
 * Sends card production requests to rpe_card_processor. The service depends on this interface, not on SQS.
 */
public interface CardProductionPublisher {

    /** Must be called inside the transaction that creates the customer; the request is delivered after commit. */
    void publish(CardProductionRequested event);
}
