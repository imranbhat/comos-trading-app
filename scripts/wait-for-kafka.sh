#!/bin/bash

echo "Waiting for Kafka to be ready..."

KAFKA_HOST=${KAFKA_HOST:-localhost}
KAFKA_PORT=${KAFKA_PORT:-9092}

until nc -z $KAFKA_HOST $KAFKA_PORT; do
  echo "Kafka is unavailable - sleeping"
  sleep 2
done

echo "Kafka is up - executing command"
exec "$@"

