package com.rpe.clientmanager.exception;

import com.rpe.clientmanager.domain.CustomerStatus;

/**
 * PUT may only block a customer. Cancelling and activating have their own endpoints.
 */
public class StatusChangeNotAllowedException extends BusinessRuleException {

    public StatusChangeNotAllowedException(CustomerStatus requested) {
        super(ErrorCode.STATUS_CHANGE_NOT_ALLOWED, "Status can't be changed to " + requested
                + " through PUT; use DELETE to cancel and POST /api/v1/customers/{id}/activate to activate");
    }
}
