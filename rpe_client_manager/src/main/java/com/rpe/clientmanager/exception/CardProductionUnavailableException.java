package com.rpe.clientmanager.exception;

/**
 * The card production request couldn't be published, so the customer was not created (the transaction is
 * rolled back). The client can simply retry.
 */
public class CardProductionUnavailableException extends CustomException {

    public CardProductionUnavailableException(Throwable cause) {
        super(ErrorCode.CARD_PRODUCTION_UNAVAILABLE,
                "Card production is temporarily unavailable; the customer was not created, please retry");
        initCause(cause);
    }
}
