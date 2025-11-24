# Trading Microservices POC

A production-grade microservices proof-of-concept demonstrating event-driven architecture using Kafka as the streaming backbone. This system simulates a trading platform with order management, execution simulation, and real-time market data.

## Architecture Overview

```
┌─────────────┐
│     UI      │ (React + WebSocket)
│   Port 3000 │
└──────┬──────┘
       │ HTTP POST
       │ WebSocket
       ▼
┌─────────────────┐
│   Order API     │ (Spring Boot 3)
│   Port 8080     │
└──────┬──────────┘
       │ Kafka Producer
       │ (orders.commands)
       ▼
┌─────────────────┐         ┌──────────────────┐
│      OMS        │────────▶│ Execution Sim    │
│   Port 8081     │         │   Port 8083      │
└──────┬──────────┘         └────────┬─────────┘
       │ Kafka Consumer              │
       │ Kafka Producer              │ Kafka Producer
       │ (orders.events)             │ (orders.events)
       ▼                             ▼
┌─────────────────────────────────────────────┐
│              Kafka Broker                   │
│            Port 9092                        │
└─────────────────────────────────────────────┘
       ▲
       │ Kafka Producer
       │ (market-data)
       │
┌──────────────┐
│ Market Sim   │ (Spring Boot 3)
│ Port 8082    │
└──────────────┘
```

## Microservices

### 1. Market Data Simulator (`market-sim`)
- **Technology**: Spring Boot 3
- **Port**: 8082
- **Responsibilities**:
  - Publishes random market ticks to `market-data` Kafka topic every 100ms
  - No REST API (only health endpoint)
  - Simulates price movements for 8 symbols (AAPL, MSFT, GOOGL, AMZN, TSLA, META, NVDA, NFLX)

### 2. Order API (`order-api`)
- **Technology**: Spring Boot 3
- **Port**: 8080
- **Responsibilities**:
  - REST endpoint: `POST /api/v1/orders` - Accepts order creation requests
  - WebSocket endpoint: `/ws/orders` - Broadcasts order events to connected clients
  - Publishes validated `OrderCommand` to `orders.commands` topic
  - Consumes `orders.events` and pushes to WebSocket clients
  - Full actuator endpoints for monitoring

### 3. Order Management Service (`oms`)
- **Technology**: Spring Boot 3
- **Port**: 8081
- **Responsibilities**:
  - Consumes `OrderCommand` from `orders.commands` topic
  - In-memory order store (ConcurrentHashMap)
  - Idempotency check (prevents duplicate order processing)
  - Publishes `OrderAccepted` events to `orders.events` topic
  - Manual Kafka consumer acknowledgment
  - Order validation

### 4. Execution Simulator (`execution-sim`)
- **Technology**: Spring Boot 3
- **Port**: 8083
- **Responsibilities**:
  - Consumes `orders.events` (filters for ACCEPTED status only)
  - Simulates execution delay (300-500ms random)
  - Publishes `FILLED` events to `orders.events` topic
  - Simulates fill price with slight variation

### 5. UI Service (`ui`)
- **Technology**: React + Vite
- **Port**: 3000
- **Responsibilities**:
  - Order placement form
  - WebSocket client for real-time order updates
  - Live event log display
  - Market ticks panel (placeholder for future enhancement)

## Event Flow

1. **Order Submission**: UI → Order API (REST) → `orders.commands` topic
2. **Order Processing**: OMS consumes command → validates → stores → publishes `OrderAccepted` → `orders.events`
3. **Order Execution**: Execution Simulator consumes ACCEPTED events → waits 300-500ms → publishes `FILLED` → `orders.events`
4. **Real-time Updates**: Order API consumer → WebSocket → UI updates

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17+ (for local development)
- Node.js 18+ (for local UI development)

### Running the Entire Stack

```bash
# Start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

The stack will be available at:
- **UI**: http://localhost:3000
- **Order API**: http://localhost:8080
- **OMS**: http://localhost:8081
- **Market Sim**: http://localhost:8082
- **Execution Sim**: http://localhost:8083
- **Kafka**: localhost:9092

### Health Checks

```bash
# Order API
curl http://localhost:8080/actuator/health

# OMS
curl http://localhost:8081/actuator/health

# Market Sim
curl http://localhost:8082/actuator/health

# Execution Sim
curl http://localhost:8083/actuator/health
```

## Usage Examples

### Create an Order via REST API

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "side": "BUY",
    "quantity": 100,
    "price": 150.50,
    "orderType": "LIMIT"
  }'
```

Response:
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Order submitted successfully"
}
```

### WebSocket Connection

Connect to WebSocket endpoint:
```javascript
const socket = new SockJS('http://localhost:8080/ws/orders');
const client = Client.over(socket);

client.connect({}, () => {
  client.subscribe('/topic/orders', (message) => {
    const event = JSON.parse(message.body);
    console.log('Order event:', event);
  });
});
```

### Event Examples

**OrderAccepted Event:**
```json
{
  "eventId": "event-uuid",
  "orderId": "order-uuid",
  "status": "ACCEPTED",
  "symbol": "AAPL",
  "side": "BUY",
  "quantity": 100,
  "price": 150.50,
  "timestamp": "2024-01-15T10:30:00Z",
  "message": "Order accepted"
}
```

**OrderFilled Event:**
```json
{
  "eventId": "event-uuid",
  "orderId": "order-uuid",
  "status": "FILLED",
  "symbol": "AAPL",
  "side": "BUY",
  "quantity": 100,
  "filledQuantity": 100,
  "price": 150.50,
  "fillPrice": 150.48,
  "timestamp": "2024-01-15T10:30:00.500Z",
  "message": "Order filled successfully"
}
```

## Load Testing

### Load Test Script

Test order submission performance:
```bash
./scripts/loadtest.sh [number_of_orders] [order_api_url]

# Example: Submit 100 orders
./scripts/loadtest.sh 100

# Example: Submit 500 orders to custom URL
./scripts/loadtest.sh 500 http://localhost:8080
```

The script measures:
- Success/failure rates
- Response times (min, avg, p50, p90, p95, p99, max)
- Throughput (orders/second)

### Latency Measurement

Measure end-to-end latency from order submission to acceptance/fill:
```bash
# Install dependencies first
cd scripts
npm install

# Run latency measurement
node measure-latency.js [number_of_orders] [order_api_url] [ws_url]

# Example: Measure 10 orders
node measure-latency.js 10

# Example: Measure 50 orders with custom URLs
node measure-latency.js 50 http://localhost:8080 http://localhost:8080
```

The script tracks:
- Latency from submission to ACCEPTED event
- Latency from submission to FILLED event
- Percentile statistics (p50, p90, p95, p99)

## Project Structure

```
trading-poc/
├── docker-compose.yml          # Kafka + all services
├── README.md                    # This file
├── services/
│   ├── market-sim/              # Market Data Simulator
│   │   ├── src/main/java/...
│   │   ├── src/main/resources/application.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── order-api/               # Order API Gateway
│   │   ├── src/main/java/...
│   │   ├── src/main/resources/application.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── oms/                     # Order Management Service
│   │   ├── src/main/java/...
│   │   ├── src/main/resources/application.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── execution-sim/           # Execution Simulator
│   │   ├── src/main/java/...
│   │   ├── src/main/resources/application.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── ui/                      # React UI
│   │   ├── src/
│   │   ├── package.json
│   │   ├── vite.config.js
│   │   └── Dockerfile
│   └── shared-schemas/          # JSON event schemas
│       ├── OrderCommand.json
│       ├── OrderEvent.json
│       └── Tick.json
└── scripts/
    ├── loadtest.sh              # Load testing script
    ├── measure-latency.js       # Latency measurement script
    └── package.json             # Script dependencies
```

## Kafka Topics

- **`market-data`**: Market tick data published by Market Simulator
- **`orders.commands`**: Order creation commands from Order API
- **`orders.events`**: Order status events (ACCEPTED, FILLED, REJECTED)

## Development

### Building Individual Services

```bash
# Market Sim
cd services/market-sim
mvn clean package

# Order API
cd services/order-api
mvn clean package

# OMS
cd services/oms
mvn clean package

# Execution Sim
cd services/execution-sim
mvn clean package

# UI
cd services/ui
npm install
npm run build
```

### Running Services Locally

Each service can be run independently:

```bash
# Set Kafka bootstrap servers
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Run service
mvn spring-boot:run
```

## Troubleshooting

### Services Not Starting

1. **Check Kafka is running**:
   ```bash
   docker-compose ps
   # Ensure zookeeper and kafka are healthy
   ```

2. **Check service logs**:
   ```bash
   docker-compose logs [service-name]
   # Example: docker-compose logs order-api
   ```

3. **Verify port availability**:
   ```bash
   # Check if ports are in use
   lsof -i :8080
   lsof -i :9092
   ```

### WebSocket Connection Issues

1. **Verify Order API is running**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Check WebSocket endpoint**:
   ```bash
   # Test WebSocket connection
   wscat -c ws://localhost:8080/ws/orders
   ```

3. **Check CORS settings** in `order-api` WebSocket configuration

### Kafka Connection Issues

1. **Verify Kafka is accessible**:
   ```bash
   docker exec -it kafka kafka-broker-api-versions --bootstrap-server localhost:9092
   ```

2. **Check topic creation**:
   ```bash
   docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
   ```

3. **View topic messages**:
   ```bash
   docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic orders.events --from-beginning
   ```

### Order Not Processing

1. **Check OMS logs**:
   ```bash
   docker-compose logs oms
   ```

2. **Verify idempotency**: Check if order ID already exists (duplicate submission)

3. **Check event flow**:
   - Order API publishes to `orders.commands`
   - OMS consumes and publishes to `orders.events`
   - Execution Sim consumes ACCEPTED events
   - Order API consumes events and pushes to WebSocket

## Key Features

- ✅ **Event-Driven Architecture**: All communication via Kafka topics
- ✅ **Microservice Independence**: Each service has its own Dockerfile and configs
- ✅ **Idempotency**: OMS prevents duplicate order processing
- ✅ **Real-time Updates**: WebSocket for live order status
- ✅ **Health Monitoring**: Actuator endpoints for all services
- ✅ **Load Testing**: Scripts for performance testing
- ✅ **Latency Measurement**: End-to-end latency tracking

## Technology Stack

- **Backend**: Spring Boot 3.x, Java 17+
- **Messaging**: Apache Kafka
- **Frontend**: React 18, Vite
- **WebSocket**: STOMP over SockJS
- **Build**: Maven, Docker
- **Containerization**: Docker Compose

## Future Enhancements

- [ ] Market data streaming to UI
- [ ] Order cancellation support
- [ ] Partial fills
- [ ] Order history persistence
- [ ] Prometheus metrics
- [ ] Grafana dashboards
- [ ] Kubernetes manifests
- [ ] Distributed tracing (Jaeger/Zipkin)

## License

This is a proof-of-concept project for demonstration purposes.

