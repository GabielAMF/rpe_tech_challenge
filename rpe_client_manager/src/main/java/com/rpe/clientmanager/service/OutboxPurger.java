package com.rpe.clientmanager.service;

import com.rpe.clientmanager.config.OutboxProperties;
import com.rpe.clientmanager.domain.OutboxStatus;
import com.rpe.clientmanager.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Deletes SENT outbox events older than app.outbox.retention (they are kept a while, without payload, to help
 * debugging). PENDING and FAILED events are never purged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPurger {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxProperties properties;
    private final Clock clock;

    @Scheduled(cron = "${app.outbox.purge-cron}")
    @Transactional
    public void purgeSent() {
        int deleted = outboxEventRepository.deleteByStatusAndSentAtBefore(
                OutboxStatus.SENT, clock.instant().minus(properties.retention()));
        if (deleted > 0) {
            log.info("Purged {} sent outbox events older than {}", deleted, properties.retention());
        }
    }
}
