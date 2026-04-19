# IANFC — Intent-Aware Network Fault Correlation

A streaming proof-of-concept that reduces raw network alarm floods into single root-cause incidents, then checks whether those incidents violate declared operational intents. Instead of getting paged 40-200 times for one broken link, operators get one alert that says what broke and whether it matters.

Built with Java 21, Spring Boot 3.3, Apache Kafka (KRaft), Redis, PostgreSQL, and Docker Compose.

---

## The problem it solves

A single upstream failure cascades. One physical link going down triggers secondary alarms across every dependent device. A NOC operator watching the console sees 40-200 raw alarms for what is fundamentally one event. On top of that, even after you've identified the root cause, "link Y is down" tells you nothing about whether link Y was carrying an SLA-critical service.

Production tools treat these as separate problems. This project treats them as one.

---

## Architecture

```
[TelemetryGenerator]
        |
        v (network.telemetry.raw)
[Kafka]
        |
        |---> [StateUpdaterConsumer]    -- device state + FSM in Redis
        |
        |---> [FaultCorrelatorConsumer] -- sliding window alarm correlation
                    |                      idempotency lock in Redis
                    |                      CorrelatedIncident -> PostgreSQL
                    v (network.faults.correlated)
             [Kafka]
                    |
                    v
             [IntentVerifierConsumer]   -- loads intents from PostgreSQL
                                           checks violation conditions
                                           IntentViolation -> PostgreSQL
```

**Why this design:**

- Redis for hot state (O(1) FSM transitions, O(log N) sliding window via `ZADD`/`ZRANGEBYSCORE`)
- PostgreSQL for durable records (intents, incidents, violations with full audit trail)
- Kafka as the event backbone — decouples the generator from consumers and gives replay capability
- Single Spring Boot app with multiple `@KafkaListener` beans — right scope for a PoC, no inter-service networking overhead

---

## Prerequisites

- Docker Desktop 4.30+ (with Apple Silicon support if on M-series Mac)
- `docker compose` v2
- That's it. The build happens inside Docker.

---

## Running it

```bash
git clone https://github.com/ashutoshrp06/ianfc.git
cd ianfc
docker compose up --build -d
```

The app waits for Kafka, Redis, and PostgreSQL to pass health checks before starting. Flyway runs migrations automatically. First startup takes 60-90 seconds depending on your machine.

Once up, open the dashboard:

**http://localhost:8080/dashboard.html**

To stop:

```bash
docker compose down
```

To wipe all data including the Postgres volume:

```bash
docker compose down -v
```

---

## Ports

| Service    | Host Port |
|------------|-----------|
| App (HTTP) | 8080      |
| Kafka      | 9092      |
| Redis      | 6379      |
| PostgreSQL | 5432      |

---

## The dashboard

Polls four API endpoints every 2.5 seconds. Shows:

- Live device states (link state, BGP state, FSM state, latency, packet loss)
- Active and historical correlated incidents with suppressed alarm counts
- Intent violations with severity and which intent was breached
- Active intents

Has a generator control panel with three scenarios: **Normal**, **Flap**, and **Cascade Failure**. Switch between dark and light mode in the top right.

---

## REST API

### Generator

```bash
# Start a scenario
curl -X POST http://localhost:8080/api/v1/generator/start \
  -H "Content-Type: application/json" \
  -d '{"scenarioType":"CASCADE_FAILURE"}'

# Valid scenarioType values: NORMAL, FLAP, CASCADE_FAILURE

# Stop
curl -X POST http://localhost:8080/api/v1/generator/stop

# Status
curl http://localhost:8080/api/v1/generator/status
```

### Device State

```bash
# All devices
curl http://localhost:8080/api/v1/state/devices

# Single device
curl http://localhost:8080/api/v1/state/devices/router-edge-01
```

### Incidents

```bash
curl http://localhost:8080/api/v1/incidents
curl http://localhost:8080/api/v1/incidents/active
curl http://localhost:8080/api/v1/incidents/{id}
```

### Intents (full CRUD)

```bash
# Create
curl -X POST http://localhost:8080/api/v1/intents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "EU-WEST BGP Adjacency SLA",
    "intentType": "BGP_ADJACENCY",
    "targetEntity": "router-edge-01",
    "targetRegion": "EU-WEST-1",
    "thresholdValue": 0,
    "thresholdUnit": "count",
    "severity": "CRITICAL"
  }'

# Valid intentType values: BGP_ADJACENCY, MAX_LATENCY, MIN_LINK_AVAILABILITY, MAX_PACKET_LOSS

# List all
curl http://localhost:8080/api/v1/intents

# Delete
curl -X DELETE http://localhost:8080/api/v1/intents/{id}
```

### Violations

```bash
curl http://localhost:8080/api/v1/violations
curl http://localhost:8080/api/v1/violations/active
```

### Health

```bash
curl http://localhost:8080/actuator/health
```

---

## End-to-end test

A shell script is included that runs the full pipeline and asserts at each step:

```bash
./test_e2e.sh
```

It creates an intent, triggers a cascade failure, waits for the 30-second correlation window, then checks that both an incident and a violation exist. Exits non-zero on failure.

Requires `jq` for JSON parsing.

---

## Checking state directly

```bash
# Redis — device FSM states
docker exec ianfc-redis redis-cli KEYS "device:fsm:*"
docker exec ianfc-redis redis-cli GET device:fsm:router-edge-01

# Redis — device state hash
docker exec ianfc-redis redis-cli HGETALL device:state:router-edge-01

# Redis — active violations index
docker exec ianfc-redis redis-cli HGETALL violations:active

# PostgreSQL — incidents
docker exec ianfc-postgres psql -U ianfc_user -d ianfc \
  -c "SELECT fault_id, root_cause_device_id, suppressed_alarm_count, status FROM correlated_incidents;"

# PostgreSQL — violations with resolve timestamps
docker exec ianfc-postgres psql -U ianfc_user -d ianfc \
  -c "SELECT violation_id, intent_id, status, violated_at, resolved_at FROM intent_violations;"
```

---

## Auto-resolve behaviour

When the generator stops or a LINK_UP/BGP_UP event heals a device, the system resolves active incidents and violations. Both `correlated_incidents` and `intent_violations` get `status=RESOLVED` and a `resolved_at` timestamp in PostgreSQL. Redis active violation index is cleaned up at the same time. The dashboard clears automatically on the next poll.

---

## Local development (without Docker for the app)

If you want to run the Spring Boot app in IntelliJ against Dockerised infrastructure:

```bash
# Start only infrastructure
docker compose up kafka redis postgres -d
```

Then run the app from IntelliJ with the default `application.properties`. The Kafka config uses a dual-listener setup: `localhost:9092` for host-based runs, `kafka:9093` for container-to-container. Both work without any config changes.

---

## Schema

Managed by Flyway. Migrations live in `src/main/resources/db/migration/`. Three tables:

- `intents` — declared operational intents (REST-managed)
- `correlated_incidents` — root-cause incidents detected by the correlator
- `intent_violations` — violations linking incidents to intents



---

## Configuration

All defaults work out of the box via Docker Compose. Environment variables can be overridden:

| Variable | Default | Description |
|----------|---------|-------------|
| `CORRELATION_WINDOW_SECONDS` | `30` | Sliding window for alarm aggregation |
| `CORRELATION_ALARM_THRESHOLD` | `3` | Minimum alarms in window to trigger correlation |
| `GENERATOR_INTERVAL_MS` | `500` | Interval between generated telemetry events (ms) |
| `SPRING_DATASOURCE_URL` | set in compose | PostgreSQL JDBC URL |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9093` | Kafka broker address |
| `SPRING_DATA_REDIS_HOST` | `ianfc-redis` | Redis hostname |

See `.env.example` for the full list.

---

## Stack

| Component  | Version     |
|------------|-------------|
| Java       | 21          |
| Spring Boot| 3.3         |
| Kafka      | 3.7.0 (KRaft, no Zookeeper) |
| Redis      | 7.2-alpine  |
| PostgreSQL | 16-alpine   |
| Flyway     | via Spring Boot BOM |