package com.rpe.clientmanager.service;

/**
 * Result of asking rpe_card_processor for a customer's card. {@code available} is false when the service
 * couldn't be reached (then {@code card} is null too), so callers can tell "no card yet" from "don't know".
 */
public record CardLookup(CardInfo card, boolean available) {

    public static CardLookup found(CardInfo card) {
        return new CardLookup(card, true);
    }

    public static CardLookup notProducedYet() {
        return new CardLookup(null, true);
    }

    public static CardLookup unavailable() {
        return new CardLookup(null, false);
    }
}
