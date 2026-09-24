package com.rpe.clientmanager.service;

import com.rpe.clientmanager.config.OutboxProperties;
import com.rpe.clientmanager.domain.OutboxEvent;
import com.rpe.clientmanager.domain.OutboxRetryPolicy;
import com.rpe.clientmanager.domain.OutboxStatus;
import com.rpe.clientmanager.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.util.List;

/**
 * Publishes pending outbox events. Delivery is <b>at least once</b>: if the process dies after SQS accepted an
 * event but before it is marked SENT, the event is sent again with the same eventId, and the consumer
 * de-duplicates on it.
 * <p>
 * Each batch runs in one transaction that holds the rows' locks while sending (bounded by app.sqs.send-timeout).
 * The batch stops at the first failure: SQS is most likely down, so there's no point trying the rest now.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventSender sender;
    private final OutboxRetryPolicy retryPolicy;
    private final OutboxProperties properties;
    private final TransactionOperations transactionOperations;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval}")
    public void relayPending() {
        transactionOperations.executeWithoutResult(status -> relayBatch());
    }

    /** @return how many events were sent */
    int relayBatch() {
        List<OutboxEvent> due = outboxEventRepository.lockDue(clock.instant(), properties.batchSize());
        int sent = 0;
        for (OutboxEvent event : due) {
            try {
                sender.send(event);
                event.markSent(clock.instant());
                sent++;
                log.info("Sent outbox event id={} eventId={} type={} customer id={}",
                        event.getId(), event.getEventId(), event.getEventType(), event.getAggregateId());
            } catch (RuntimeException ex) {
                event.recordFailure(describe(ex), clock.instant(), retryPolicy);
                logFailure(event, ex);
                break;
            }
        }
        return sent;
    }

    private static void logFailure(OutboxEvent event, RuntimeException ex) {
        if (event.getStatus() == OutboxStatus.FAILED) {
            log.error("Giving up on outbox event id={} eventId={} customer id={} after {} attempts: {}. "
                            + "It stays FAILED until resent manually (see README).",
                    event.getId(), event.getEventId(), event.getAggregateId(), event.getAttempts(), describe(ex));
        } else {
            log.warn("Could not send outbox event id={} (attempt {}), retrying at {}: {}",
                    event.getId(), event.getAttempts(), event.getNextAttemptAt(), describe(ex));
        }
    }

    /** Class and message of the root cause: says what went wrong without including the payload. */
    private static String describe(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
