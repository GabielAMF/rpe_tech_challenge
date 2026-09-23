#!/bin/bash
# Runs automatically when LocalStack is ready.
# Creates the queue rpe_client_manager publishes to (consumed by rpe_card_processor) and its dead-letter queue.
set -euo pipefail

QUEUE="${CLIENT_MANAGER_QUEUE:-rpe-client-manager-queue}"
DLQ="${QUEUE}-dlq"

awslocal sqs create-queue --queue-name "$DLQ"
DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$(awslocal sqs get-queue-url --queue-name "$DLQ" --query QueueUrl --output text)" \
  --attribute-names QueueArn --query Attributes.QueueArn --output text)

awslocal sqs create-queue --queue-name "$QUEUE" \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"

echo "SQS queues created: $QUEUE, $DLQ"
