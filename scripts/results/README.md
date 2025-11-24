# Load Test Results

This directory contains load test results for comparing performance between different messaging systems (Redpanda vs Kafka).

## File Naming Convention

Results are saved with the following format:
```
loadtest_{MESSAGING_SYSTEM}_{TIMESTAMP}.txt
```

Example:
- `loadtest_Redpanda_20251124_134119.txt`
- `loadtest_Kafka_20251124_140530.txt`

## Result File Format

Each result file contains:
- Timestamp of the test
- Messaging system used (Redpanda/Kafka)
- Test parameters (target URL, number of orders)
- Summary statistics:
  - Total orders sent
  - Success/failure counts
  - Success rate
  - Total duration
  - Average throughput (orders/sec)
- Response time statistics:
  - Min, Avg, Max
  - Percentiles: P50, P90, P95, P99

## Comparing Results

### Manual Comparison

1. Run load tests with both systems:
   ```bash
   # With Redpanda
   ./scripts/loadtest.sh 100
   
   # Switch to Kafka, then run again
   ./scripts/loadtest.sh 100
   ```

2. View results:
   ```bash
   cat scripts/results/loadtest_Redpanda_*.txt
   cat scripts/results/loadtest_Kafka_*.txt
   ```

### Using the Comparison Script

Run the comparison script to see side-by-side comparison:
```bash
./scripts/compare-results.sh
```

## Key Metrics to Compare

- **Success Rate**: Should be 100% for both systems
- **Average Throughput**: Orders per second (higher is better)
- **Response Time P50**: Median response time (lower is better)
- **Response Time P99**: 99th percentile (lower is better, shows worst-case performance)
- **Total Duration**: Time to complete all orders (lower is better)

## Notes

- Results are saved automatically when running `loadtest.sh`
- Files are timestamped to allow multiple runs
- The messaging system is auto-detected from running Docker containers

