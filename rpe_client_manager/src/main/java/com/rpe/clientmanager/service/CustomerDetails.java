package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.Customer;

/** A customer together with its card (and the card's product) from rpe_card_processor. */
public record CustomerDetails(Customer customer, CardLookup card) {
}
