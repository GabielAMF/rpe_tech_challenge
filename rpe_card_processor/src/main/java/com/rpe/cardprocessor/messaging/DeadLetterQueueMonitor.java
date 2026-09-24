package com.rpe.cardprocessor.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.concurrent.TimeUnit;

/**
 * Watches the dead-letter queue without consuming it: consuming would delete the messages that need to be
 * investigated and redriven (see README). Logs at ERROR while the DLQ isn't empty, the hook for an alert; in AWS
 * this would be a CloudWatch alarm on the queue's ApproximateNumberOfMessagesVisible.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sqs.dlq-monitor.enabled", havingValue = "true", matchIfMissing = true)
public class DeadLetterQueueMonitor {

    private static final long TIMEOUT_SECONDS = 5;

    private final SqsAsyncClient sqs;
    private final String deadLetterQueue;

    public DeadLetterQueueMonitor(SqsAsyncClient sqs, @Value("${app.sqs.client-manager-dlq}") String deadLetterQueue) {
        this.sqs = sqs;
        this.deadLetterQueue = deadLetterQueue;
    }

    @Scheduled(fixedDelayString = "${app.sqs.dlq-monitor.interval}", initialDelayString = "${app.sqs.dlq-monitor.interval}")
    public void check() {
        try {
            int messages = countMessages();
            if (messages > 0) {
                log.error("Dead-letter queue {} holds {} card production message(s) that failed 3 times; "
                        + "investigate and redrive them (see README)", deadLetterQueue, messages);
            }
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Could not read the size of dead-letter queue {}: {}", deadLetterQueue, ex.toString());
        }
    }

    /** Approximate number of messages in the DLQ (SQS counts are eventually consistent). */
    public int countMessages() throws Exception {
        String url = sqs.getQueueUrl(r -> r.queueName(deadLetterQueue))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).queueUrl();
        String count = sqs.getQueueAttributes(r -> r.queueUrl(url)
                        .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
        return count == null ? 0 : Integer.parseInt(count);
    }
}
