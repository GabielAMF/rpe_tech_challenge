package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.domain.Card;

/**
 * A card plus the current status of its product in rpe_catalog. The card keeps a snapshot of the product's id,
 * name and description from issue time; the status is read live (through the cache) because a product can be
 * cancelled after the card was issued. {@code productStatus} is null when the catalog can't say.
 */
public record CardDetails(Card card, String productStatus) {
}
