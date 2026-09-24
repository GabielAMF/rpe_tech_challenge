package com.rpe.clientmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.rpe.clientmanager.domain.OutboxEvent;
import com.rpe.clientmanager.domain.OutboxStatus;
import com.rpe.clientmanager.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.rpe.clientmanager.CustomerFixtures.NOW;
import static com.rpe.clientmanager.CustomerFixtures.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxCardProductionPublisherTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Mock
    private OutboxEventRepository repository;

    @Test
    void storesTheMessageJsonAsAPendingOutboxEvent() throws Exception {
        UUID customerId = UUID.randomUUID();
        CardProductionRequested event = CardProductionRequested.of(customer(customerId), "score=780", NOW);
        var publisher = new OutboxCardProductionPublisher(repository, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));

        publisher.publish(event);

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());
        OutboxEvent outbox = saved.getValue();
        assertThat(outbox.getEventId()).isEqualTo(event.eventId());
        assertThat(outbox.getEventType()).isEqualTo("CARD_PRODUCTION_REQUESTED");
        assertThat(outbox.getAggregateId()).isEqualTo(customerId);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(NOW);

        JsonNode payload = objectMapper.readTree(outbox.getPayload());
        assertThat(payload.get("eventId").asText()).isEqualTo(event.eventId().toString());
        assertThat(payload.get("customerId").asText()).isEqualTo(customerId.toString());
        assertThat(payload.get("cpf").asText()).isEqualTo("12345678909");
        assertThat(payload.get("creditInfo").asText()).isEqualTo("score=780");
    }
}
