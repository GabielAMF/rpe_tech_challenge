package com.rpe.clientmanager.service;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer's card as reported by rpe_card_processor, including the product it was issued for.
 * Statuses are kept as strings: they belong to other services and may gain values we don't know yet.
 */
public record CardInfo(UUID cardId, String status, String maskedNumber, Product product, Instant createdAt) {

    public record Product(UUID id, String name, String description, String status) {
    }
}
