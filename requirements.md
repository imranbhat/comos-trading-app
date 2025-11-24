Implement a production-grade microservices POC using event-driven architecture with Redpanda (Kafka-compatible) as the streaming backbone.
Each component must be a fully independent microservice with its own Dockerfile, configs, schema, and CICD workflow.

1. Architecture — Create these Microservices
1. Market Data Simulator


Spring Boot 3


Publishes random ticks to Redpanda topic market-data


No REST API


Exposes /actuator/health


2. Order API (Edge/API Gateway service)


Spring Boot 3


REST endpoint: POST /api/v1/orders


Publishes validated OrderCommand → topic orders.commands


Opens WebSocket endpoint /ws/orders for UI


Consumers push → WebSocket


Exposes /actuator/*


3. OMS (Order Management Service)


Spring Boot 3


Consumes from orders.commands


In-memory order store (ConcurrentHashMap)


Emits OrderAccepted → topic orders.events


Emits OrderStatusUpdated after execution → topic


Manual consumer ACK


Idempotency required


4. Execution Simulator


Spring Boot 3


Consumes orders.events (only events with status=ACCEPTED)


Wait 300–500ms


Emits FILLED event


5. UI Service (React or Next.js)


Form → REST call to Order API


WebSocket subscription to order updates


Live event log window


Ticks panel (market-data streaming optional later)



2. Repository Structure (Monorepo but Microservices)
trading-poc/
├── docker-compose.yml              # Redpanda + all services
├── README.md
├── services/
│   ├── market-sim/
│   ├── order-api/
│   ├── oms/
│   ├── execution-sim/
│   └── ui/
└── scripts/
    ├── loadtest.sh
    └── measure-latency.js

Each microservice has:
service-name/
 ├─ src/main/java/...
 ├─ src/main/resources/application.yml
 ├─ Dockerfile
 └─ pom.xml / build.gradle


3. Instructions for Cursor — Build EVERYTHING Automatically
Step-by-Step Tasks Cursor Must Perform

STEP 1 — Setup docker-compose with Redpanda
Create docker-compose.yml containing:


Kafka


All Spring Boot services


UI service


Expose:


Redpanda broker → 9092 (internal), 19092 (external)


Order API → 8080


OMS → 8081


MarketSim → 8082


ExecutionSim → 8083


UI → 3000


Add depends_on and a wait-for-redpanda.sh script.

STEP 2 — Define Event Schemas (JSON)
Create directory:
services/shared-schemas/

Files:


OrderCommand.json


OrderEvent.json


Tick.json


Cursor must generate simple JSON schemas for POC.

STEP 3 — Implement Each Microservice Fully
3A — MarketSim


Scheduled job every 100 ms


Publishes Tick events


Uses KafkaTemplate


Dockerfile + health endpoints


3B — Order API


REST POST → Validate → Publish to Redpanda


WebSocket endpoint broadcasting:


order events


tick events (optional)




Redpanda listener pushes to Sinks.Many


Dockerfile


3C — OMS


KafkaListener → orders.commands


Validate, idempotency


Publish ACCEPTED event


Save order in local map


Manual ACK


Dockerfile


3D — Execution Simulator


KafkaListener → orders.events


Only process ACCEPTED


Emit FILLED


Dockerfile


3E — UI


React (Vite)


WebSocket client


Form to place orders


Real-time event panel



STEP 4 — Scripts
scripts/loadtest.sh


Loop POST 100–500 orders


Measure response time


scripts/measure-latency.js


Subscribe via WebSocket


Record orderAccepted timestamp


Output p50/p90/p95/p99 latency



STEP 5 — README Requirements
Cursor must generate a full README that includes:


Architecture overview


Microservice responsibilities


How to run the entire stack with one command


cURL examples


WebSocket usage


Troubleshooting



STEP 6 — Optional Enhancements
Cursor may generate stubs for:


Prometheus/Grafana


Redpanda Console (via Kafka UI or rpk CLI)


Kubernetes manifests (NOT required)



4. Cursor Must Follow These Rules
✔ Strict Microservice Separation
No shared Java module.
No shared Spring Boot project.
Only schema folder is shared.
✔ Independent Dockerfiles
Every service builds its own container.
✔ Independent Redpanda clients (using Spring Kafka library)
Each service has its own Redpanda/Kafka configs.
✔ Clean code
Use controllers, services, producers, listeners, DTOs.

5. Deliverables Cursor Should Output
Cursor should generate:
✔ Full working multi-service codebase
✔ docker-compose that actually works end-to-end
✔ UI + WebSocket demo
✔ Load-test scripts
✔ Diagrams in README
✔ Code well-commented

6. Final Instruction
Cursor: Build the entire architecture EXACTLY as described.
Generate all folders, files, code, configs, Dockerfiles, Compose, tests, and README.

