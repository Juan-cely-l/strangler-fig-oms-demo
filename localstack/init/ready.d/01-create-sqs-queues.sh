#!/bin/sh
set -e

awslocal sqs create-queue --queue-name order-created-dlq >/dev/null || true
awslocal sqs create-queue --queue-name inventory-result-dlq >/dev/null || true

DLQ_URL="$(awslocal sqs get-queue-url --queue-name order-created-dlq --query QueueUrl --output text)"
DLQ_ARN="$(awslocal sqs get-queue-attributes \
  --queue-url "$DLQ_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)"

awslocal sqs create-queue \
  --queue-name order-created-queue \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}" \
  >/dev/null || true

RESULT_DLQ_URL="$(awslocal sqs get-queue-url --queue-name inventory-result-dlq --query QueueUrl --output text)"
RESULT_DLQ_ARN="$(awslocal sqs get-queue-attributes \
  --queue-url "$RESULT_DLQ_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)"

awslocal sqs create-queue \
  --queue-name inventory-result-queue \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$RESULT_DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}" \
  >/dev/null || true

echo "LocalStack SQS queues ready: order-created-queue, order-created-dlq, inventory-result-queue, inventory-result-dlq"
