package com.rpe.clientmanager.service;

import java.util.UUID;

/**
 * Reads card information from rpe_card_processor. Never throws: failures become {@link CardLookup#unavailable()}.
 */
public interface CardInfoGateway {

    CardLookup findByCustomerId(UUID customerId);
}
