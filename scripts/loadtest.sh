#!/bin/bash

# Load testing script for Order API
# Usage: ./loadtest.sh [number_of_orders] [order_api_url]

ORDER_API_URL=${2:-http://localhost:8080}
NUM_ORDERS=${1:-100}

# Detect messaging system
MESSAGING_SYSTEM="Unknown"
if docker ps --format '{{.Names}}' | grep -q "^redpanda$"; then
    MESSAGING_SYSTEM="Redpanda"
elif docker ps --format '{{.Names}}' | grep -q "^kafka$"; then
    MESSAGING_SYSTEM="Kafka"
fi

# Create results directory if it doesn't exist
RESULTS_DIR="scripts/results"
mkdir -p "$RESULTS_DIR"

# Generate timestamp and filename
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULT_FILE="$RESULTS_DIR/loadtest_${MESSAGING_SYSTEM}_${TIMESTAMP}.txt"

echo "=========================================="
echo "Load Testing Order API"
echo "=========================================="
echo "Target: $ORDER_API_URL/api/v1/orders"
echo "Number of orders: $NUM_ORDERS"
echo "Messaging System: $MESSAGING_SYSTEM"
echo "Results will be saved to: $RESULT_FILE"
echo "=========================================="
echo ""

# Start logging to file
{
    echo "=========================================="
    echo "Load Test Results"
    echo "=========================================="
    echo "Timestamp: $(date)"
    echo "Messaging System: $MESSAGING_SYSTEM"
    echo "Target: $ORDER_API_URL/api/v1/orders"
    echo "Number of orders: $NUM_ORDERS"
    echo "=========================================="
    echo ""
} > "$RESULT_FILE"

# Symbols to use
SYMBOLS=("AAPL" "MSFT" "GOOGL" "AMZN" "TSLA" "META" "NVDA" "NFLX")
SIDES=("BUY" "SELL")
ORDER_TYPES=("LIMIT" "MARKET")

# Arrays to store response times
declare -a response_times

# Function to generate random order
generate_order() {
    local symbol=${SYMBOLS[$RANDOM % ${#SYMBOLS[@]}]}
    local side=${SIDES[$RANDOM % ${#SIDES[@]}]}
    local quantity=$((RANDOM % 1000 + 1))
    local price=$(awk "BEGIN {printf \"%.2f\", ($RANDOM % 500 + 50) / 1}")
    local order_type=${ORDER_TYPES[$RANDOM % ${#ORDER_TYPES[@]}]}
    
    echo "{\"symbol\":\"$symbol\",\"side\":\"$side\",\"quantity\":$quantity,\"price\":$price,\"orderType\":\"$order_type\"}"
}

# Function to send order and measure time
send_order() {
    local order_data=$(generate_order)
    local start_time=$(date +%s%N)
    
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$order_data" \
        "$ORDER_API_URL/api/v1/orders")
    
    local end_time=$(date +%s%N)
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | sed '$d')
    
    local duration=$(( (end_time - start_time) / 1000000 )) # Convert to milliseconds
    
    if [ "$http_code" -eq 202 ]; then
        echo "✓ Order submitted successfully (${duration}ms)"
        response_times+=($duration)
        return 0
    else
        echo "✗ Order failed with HTTP $http_code (${duration}ms)"
        echo "  Response: $response_body"
        return 1
    fi
}

# Statistics variables
success_count=0
failure_count=0
total_time=0

# Start time
overall_start=$(date +%s%N)

# Send orders
for i in $(seq 1 $NUM_ORDERS); do
    echo -n "[$i/$NUM_ORDERS] "
    if send_order; then
        ((success_count++))
    else
        ((failure_count++))
    fi
    
    # Small delay to avoid overwhelming the system
    sleep 0.01
done

# End time
overall_end=$(date +%s%N)
overall_duration=$(( (overall_end - overall_start) / 1000000 ))

# Calculate statistics
if [ ${#response_times[@]} -gt 0 ]; then
    # Sort response times
    IFS=$'\n' sorted=($(sort -n <<<"${response_times[*]}"))
    unset IFS
    
    # Calculate percentiles
    count=${#sorted[@]}
    p50_idx=$((count * 50 / 100))
    p90_idx=$((count * 90 / 100))
    p95_idx=$((count * 95 / 100))
    p99_idx=$((count * 99 / 100))
    
    p50=${sorted[$p50_idx]}
    p90=${sorted[$p90_idx]}
    p95=${sorted[$p95_idx]}
    p99=${sorted[$p99_idx]}
    
    # Calculate average
    sum=0
    for time in "${sorted[@]}"; do
        sum=$((sum + time))
    done
    avg=$((sum / count))
    
    # Min and Max
    min=${sorted[0]}
    max_idx=$((count - 1))
    max=${sorted[$max_idx]}
fi

# Calculate success rate and throughput
if [ $NUM_ORDERS -gt 0 ]; then
    success_rate=$(awk "BEGIN {printf \"%.2f\", ($success_count / $NUM_ORDERS) * 100}")
else
    success_rate="0.00"
fi

if [ $overall_duration -gt 0 ]; then
    throughput=$(awk "BEGIN {printf \"%.2f\", ($NUM_ORDERS / ($overall_duration / 1000))}")
else
    throughput="0.00"
fi

# Print summary
echo ""
echo "=========================================="
echo "Load Test Summary"
echo "=========================================="
echo "Total orders sent: $NUM_ORDERS"
echo "Successful: $success_count"
echo "Failed: $failure_count"
echo "Success rate: ${success_rate}%"
echo "Total duration: ${overall_duration}ms"
echo "Average throughput: ${throughput} orders/sec"
echo ""

if [ ${#response_times[@]} -gt 0 ]; then
    echo "Response Time Statistics (ms):"
    echo "  Min:    ${min}ms"
    echo "  Avg:    ${avg}ms"
    echo "  P50:    ${p50}ms"
    echo "  P90:    ${p90}ms"
    echo "  P95:    ${p95}ms"
    echo "  P99:    ${p99}ms"
    echo "  Max:    ${max}ms"
fi

echo "=========================================="

# Save summary to file
{
    echo ""
    echo "=========================================="
    echo "Load Test Summary"
    echo "=========================================="
    echo "Total orders sent: $NUM_ORDERS"
    echo "Successful: $success_count"
    echo "Failed: $failure_count"
    echo "Success rate: ${success_rate}%"
    echo "Total duration: ${overall_duration}ms"
    echo "Average throughput: ${throughput} orders/sec"
    echo ""
    
    if [ ${#response_times[@]} -gt 0 ]; then
        echo "Response Time Statistics (ms):"
        echo "  Min:    ${min}ms"
        echo "  Avg:    ${avg}ms"
        echo "  P50:    ${p50}ms"
        echo "  P90:    ${p90}ms"
        echo "  P95:    ${p95}ms"
        echo "  P99:    ${p99}ms"
        echo "  Max:    ${max}ms"
    fi
    
    echo "=========================================="
} >> "$RESULT_FILE"

echo ""
echo "Results saved to: $RESULT_FILE"

