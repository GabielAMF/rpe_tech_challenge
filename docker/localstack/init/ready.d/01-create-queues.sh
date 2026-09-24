#!/bin/bash
# Runs automatically when LocalStack is ready.
# Creates the queue rpe_client_manager publishes to (consumed by rpe_card_processor) and its dead-letter queue.
set -euo pipefail

QUEUE="${CLIENT_MANAGER_QUEUE:-rpe-client-manager-queue}"
DLQ="${QUEUE}-dlq"

# Dead messages are kept 14 days (the SQS maximum) so they can be investigated and redriven.
awslocal sqs create-queue --queue-name "$DLQ" --attributes MessageRetentionPeriod=1209600
DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$(awslocal sqs get-queue-url --queue-name "$DLQ" --query QueueUrl --output text)" \
  --attribute-names QueueArn --query Attributes.QueueArn --output text)

# After 3 failed receives a message goes to the DLQ. rpe_card_processor spaces the attempts out (exponential backoff,
# see its SqsConfig); VisibilityTimeout is the time a message may take to be processed before it is redelivered.
awslocal sqs create-queue --queue-name "$QUEUE" \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"
awslocal sqs set-queue-attributes \
  --queue-url "$(awslocal sqs get-queue-url --queue-name "$QUEUE" --query QueueUrl --output text)" \
  --attributes VisibilityTimeout=30

echo "SQS queues created: $QUEUE, $DLQ"
