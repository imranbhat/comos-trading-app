#!/bin/bash

echo "Waiting for Redpanda to be ready..."

REDPANDA_HOST=${REDPANDA_HOST:-localhost}
REDPANDA_PORT=${REDPANDA_PORT:-9092}

until nc -z $REDPANDA_HOST $REDPANDA_PORT; do
  echo "Redpanda is unavailable - sleeping"
  sleep 2
done

echo "Redpanda is up - executing command"
exec "$@"

