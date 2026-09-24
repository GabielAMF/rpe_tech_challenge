package com.rpe.clientmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpe.clientmanager.domain.OutboxEvent;
import com.rpe.clientmanager.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Saves the card production request in the outbox, in the caller's transaction: the customer and its request are
 * committed together or not at all. {@link OutboxRelay} sends it to SQS afterwards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCardProductionPublisher implements CardProductionPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(CardProductionRequested event) {
        outboxEventRepository.save(new OutboxEvent(
                event.eventId(), event.eventType(), event.customerId(), toJson(event), clock.instant()));
        log.info("Stored {} in the outbox", event);
    }

    private String toJson(CardProductionRequested event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            // The message would contain the payload: keep only the type.
            throw new IllegalStateException("Could not serialize " + event + " (" + ex.getClass().getSimpleName() + ")");
        }
    }
}
