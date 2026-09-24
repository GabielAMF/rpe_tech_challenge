package com.rpe.cardprocessor.exception;

import java.util.UUID;

/** The customer has no card (yet): the card production message may still be in the queue. */
public class CardNotFoundException extends CustomException {

    public CardNotFoundException(UUID customerId) {
        super(ErrorCode.CARD_NOT_FOUND, "No card found for customer " + customerId);
    }
}
