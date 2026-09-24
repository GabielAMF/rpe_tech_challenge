package com.rpe.clientmanager;

import com.rpe.clientmanager.domain.OutboxEvent;
import com.rpe.clientmanager.repository.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The outbox payload is encrypted in the database and decrypted on read. The event is scheduled a day ahead so the
 * running relay leaves it alone. Needs the infrastructure from docker-compose.yml.
 */
@SpringBootTest
class OutboxPersistenceIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID customerId = UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        outboxEventRepository.deleteAll(outboxEventRepository.findByAggregateId(customerId));
    }

    @Test
    void payloadIsEncryptedAtRest() {
        String json = "{\"customerName\":\"Maria Silva\",\"cpf\":\"12345678909\",\"creditInfo\":\"score=780\"}";
        OutboxEvent saved = outboxEventRepository.saveAndFlush(new OutboxEvent(UUID.randomUUID(),
                "CARD_PRODUCTION_REQUESTED", customerId, json, Instant.now().plus(Duration.ofDays(1))));

        String stored = jdbcTemplate.queryForObject(
                "SELECT payload_encrypted FROM outbox_event WHERE id = ?", String.class, saved.getId());
        assertThat(stored).doesNotContain("12345678909").doesNotContain("Maria").doesNotContain("score");

        assertThat(outboxEventRepository.findById(saved.getId()).orElseThrow().getPayload()).isEqualTo(json);
    }
}
