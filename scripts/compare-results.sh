#!/bin/bash

# Comparison script for load test results
# Usage: ./compare-results.sh

RESULTS_DIR="scripts/results"

if [ ! -d "$RESULTS_DIR" ]; then
    echo "Results directory not found: $RESULTS_DIR"
    exit 1
fi

# Find latest results for each system
LATEST_REDPANDA=$(ls -t "$RESULTS_DIR"/loadtest_Redpanda_*.txt 2>/dev/null | head -1)
LATEST_KAFKA=$(ls -t "$RESULTS_DIR"/loadtest_Kafka_*.txt 2>/dev/null | head -1)

if [ -z "$LATEST_REDPANDA" ] && [ -z "$LATEST_KAFKA" ]; then
    echo "No results found in $RESULTS_DIR"
    exit 1
fi

echo "=========================================="
echo "Load Test Results Comparison"
echo "=========================================="
echo ""

if [ -n "$LATEST_REDPANDA" ]; then
    echo "📊 REDPANDA Results:"
    echo "   File: $(basename $LATEST_REDPANDA)"
    echo "   $(grep "Timestamp:" $LATEST_REDPANDA)"
    echo ""
    grep -A 20 "Load Test Summary" $LATEST_REDPANDA | head -15
    echo ""
fi

if [ -n "$LATEST_KAFKA" ]; then
    echo "📊 KAFKA Results:"
    echo "   File: $(basename $LATEST_KAFKA)"
    echo "   $(grep "Timestamp:" $LATEST_KAFKA)"
    echo ""
    grep -A 20 "Load Test Summary" $LATEST_KAFKA | head -15
    echo ""
fi

if [ -n "$LATEST_REDPANDA" ] && [ -n "$LATEST_KAFKA" ]; then
    echo "=========================================="
    echo "Quick Comparison"
    echo "=========================================="
    
    # Extract key metrics
    RP_SUCCESS=$(grep "Success rate:" $LATEST_REDPANDA | awk '{print $NF}')
    K_SUCCESS=$(grep "Success rate:" $LATEST_KAFKA | awk '{print $NF}')
    
    RP_THROUGHPUT=$(grep "Average throughput:" $LATEST_REDPANDA | awk '{print $NF}')
    K_THROUGHPUT=$(grep "Average throughput:" $LATEST_KAFKA | awk '{print $NF}')
    
    RP_P50=$(grep "P50:" $LATEST_REDPANDA | awk '{print $NF}' | sed 's/ms//')
    K_P50=$(grep "P50:" $LATEST_KAFKA | awk '{print $NF}' | sed 's/ms//')
    
    RP_P99=$(grep "P99:" $LATEST_REDPANDA | awk '{print $NF}' | sed 's/ms//')
    K_P99=$(grep "P99:" $LATEST_KAFKA | awk '{print $NF}' | sed 's/ms//')
    
    echo "Metric              | Redpanda    | Kafka"
    echo "--------------------|-------------|-------------"
    printf "Success Rate        | %-11s | %s\n" "$RP_SUCCESS" "$K_SUCCESS"
    printf "Throughput (ord/s) | %-11s | %s\n" "$RP_THROUGHPUT" "$K_THROUGHPUT"
    printf "P50 Latency (ms)   | %-11s | %s\n" "${RP_P50}ms" "${K_P50}ms"
    printf "P99 Latency (ms)   | %-11s | %s\n" "${RP_P99}ms" "${K_P99}ms"
    echo ""
fi

echo "=========================================="
echo "All Results Files:"
ls -lh "$RESULTS_DIR"/loadtest_*.txt 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}'
echo "=========================================="

