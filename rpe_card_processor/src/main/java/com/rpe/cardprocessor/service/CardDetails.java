package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.domain.Card;

/**
 * A card plus its product as rpe_catalog describes it now (read through the Redis cache). If the catalog can't
 * answer, {@code product} is the snapshot stored with the card at issue time, with a null status.
 */
public record CardDetails(Card card, CatalogProduct product) {
}
