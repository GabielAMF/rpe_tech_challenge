package com.rpe.clientmanager.controller;

import com.rpe.clientmanager.controller.dto.CustomerResponse;
import com.rpe.clientmanager.domain.Customer;
import org.springframework.stereotype.Component;

/** Converts domain customers into the public API representation. */
@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getCpf(),
                customer.getBirthDate(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
