package com.rpe.clientmanager.controller;

import com.rpe.clientmanager.controller.dto.CustomerDetailsResponse;
import com.rpe.clientmanager.controller.dto.CustomerResponse;
import com.rpe.clientmanager.domain.Customer;
import com.rpe.clientmanager.service.CardInfo;
import com.rpe.clientmanager.service.CustomerDetails;
import org.springframework.stereotype.Component;

/** Converts domain customers (and their card info) into the public API representation. */
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

    public CustomerDetailsResponse toDetailsResponse(CustomerDetails details) {
        Customer customer = details.customer();
        return new CustomerDetailsResponse(
                customer.getId(),
                customer.getName(),
                customer.getCpf(),
                customer.getBirthDate(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                toCardResponse(details.card().card()),
                details.card().available());
    }

    private static CustomerDetailsResponse.CardResponse toCardResponse(CardInfo card) {
        if (card == null) {
            return null;
        }
        CardInfo.Product product = card.product();
        return new CustomerDetailsResponse.CardResponse(
                card.cardId(),
                card.status(),
                card.maskedNumber(),
                product == null ? null
                        : new CustomerDetailsResponse.ProductResponse(product.id(), product.name(), product.description(), product.status()),
                card.createdAt());
    }
}
