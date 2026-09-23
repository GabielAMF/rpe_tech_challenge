package com.rpe.clientmanager.exception;

import com.rpe.clientmanager.domain.Cpf;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * Business rule: a CPF belongs to one customer forever. If that customer is cancelled, reactivate it
 * instead of creating a new one; its id is returned as {@code customerId}.
 */
@Getter
public class CancelledCustomerExistsException extends BusinessRuleException {

    private final UUID customerId;

    public CancelledCustomerExistsException(UUID customerId, Cpf cpf) {
        super(ErrorCode.CANCELLED_CUSTOMER_EXISTS,
                "A customer with CPF " + cpf.masked() + " already exists but is cancelled; reactivate it with POST /api/v1/customers/"
                        + customerId + "/activate");
        this.customerId = customerId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("customerId", customerId);
    }
}
