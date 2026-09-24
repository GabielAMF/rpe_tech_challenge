package com.rpe.cardprocessor.exception;

/** A card production message missing required data. Retrying won't help; it ends up in the DLQ. */
public class InvalidCardProductionRequestException extends CustomException {

    public InvalidCardProductionRequestException(String reason) {
        super(ErrorCode.INVALID_CARD_PRODUCTION_REQUEST, "Invalid card production request: " + reason);
    }
}
