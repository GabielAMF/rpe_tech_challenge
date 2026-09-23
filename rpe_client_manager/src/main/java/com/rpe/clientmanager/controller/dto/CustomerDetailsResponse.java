package com.rpe.clientmanager.controller.dto;

import com.rpe.clientmanager.domain.CustomerStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * GET /customers/{id}: the customer, its card and the card's product.
 * {@code card} is null when there is no card yet or when rpe_card_processor couldn't be reached;
 * {@code cardInfoAvailable} tells the two apart (false = couldn't be reached).
 */
public record CustomerDetailsResponse(
        UUID id,
        String name,
        String cpf,
        LocalDate birthDate,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt,
        CardResponse card,
        boolean cardInfoAvailable
) {

    public record CardResponse(UUID cardId, String status, String maskedNumber, ProductResponse product, Instant createdAt) {
    }

    public record ProductResponse(UUID id, String name, String description, String status) {
    }
}
