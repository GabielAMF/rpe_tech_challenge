package com.rpe.clientmanager.messaging;

import com.rpe.clientmanager.exception.CardProductionUnavailableException;
import com.rpe.clientmanager.service.CardProductionPublisher;
import com.rpe.clientmanager.service.CardProductionRequested;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes card production requests to {@code app.sqs.client-manager-queue} (consumed by rpe_card_processor).
 * <p>
 * Called inside the customer-creation transaction: if sending fails, the exception rolls the customer back and
 * the API answers 503, so a customer never exists without its card request.
 * <p>
 * TODO(outbox): temporary solution, to be improved for the final product. Replace the synchronous send with a
 *  transactional outbox (save the event in the same
 *  transaction as the customer, publish it from a scheduled relay with retries). The current approach still has
 *  two gaps: the database commit can fail after the message was sent (a message for a customer that doesn't
 *  exist), and a send that times out here may still arrive later. See README "Card production".
 */
@Slf4j
@Component
public class SqsCardProductionPublisher implements CardProductionPublisher {

    private final SqsTemplate sqsTemplate;
    private final String queue;
    private final long timeoutMillis;

    public SqsCardProductionPublisher(SqsTemplate sqsTemplate,
                                      @Value("${app.sqs.client-manager-queue}") String queue,
                                      @Value("${app.sqs.send-timeout:5s}") Duration timeout) {
        this.sqsTemplate = sqsTemplate;
        this.queue = queue;
        this.timeoutMillis = timeout.toMillis();
    }

    @Override
    public void publish(CardProductionRequested event) {
        try {
            // Bounded wait, so an unreachable SQS can't hold the database transaction open indefinitely.
            sqsTemplate.sendAsync(to -> to.queue(queue).payload(event))
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);
            log.info("Published {} to {}", event, queue);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw failed(event, ex);
        } catch (ExecutionException | TimeoutException | RuntimeException ex) {
            throw failed(event, ex);
        }
    }

    private CardProductionUnavailableException failed(CardProductionRequested event, Exception ex) {
        log.error("Could not publish {} to {}: {}", event, queue, ex.toString());
        return new CardProductionUnavailableException(ex);
    }
}
