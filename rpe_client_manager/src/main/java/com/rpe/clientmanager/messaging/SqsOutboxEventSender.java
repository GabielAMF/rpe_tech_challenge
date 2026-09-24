package com.rpe.clientmanager.messaging;

import com.rpe.clientmanager.domain.OutboxEvent;
import com.rpe.clientmanager.exception.EventPublishingException;
import com.rpe.clientmanager.service.CardProductionRequested;
import com.rpe.clientmanager.service.OutboxEventSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sends outbox events to SQS. The payload is already the JSON message (see SqsConfig: no Java type header), so
 * it goes out unchanged. Card production requests go to {@code app.sqs.client-manager-queue}.
 */
@Component
public class SqsOutboxEventSender implements OutboxEventSender {

    private final SqsTemplate sqsTemplate;
    private final String queue;
    private final long timeoutMillis;

    public SqsOutboxEventSender(SqsTemplate sqsTemplate,
                                @Value("${app.sqs.client-manager-queue}") String queue,
                                @Value("${app.sqs.send-timeout:5s}") Duration timeout) {
        this.sqsTemplate = sqsTemplate;
        this.queue = queue;
        this.timeoutMillis = timeout.toMillis();
    }

    @Override
    public void send(OutboxEvent event) {
        if (!CardProductionRequested.EVENT_TYPE.equals(event.getEventType())) {
            throw new EventPublishingException(event.getEventId(), "no queue for event type " + event.getEventType(), null);
        }
        try {
            // Bounded wait: the relay holds the event's row lock while sending.
            sqsTemplate.sendAsync(to -> to.queue(queue).payload(event.getPayload()))
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EventPublishingException(event.getEventId(), "interrupted", ex);
        } catch (ExecutionException | TimeoutException | RuntimeException ex) {
            throw new EventPublishingException(event.getEventId(), "SQS did not accept it", ex);
        }
    }
}
