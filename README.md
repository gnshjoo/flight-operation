# Korean Air Flight Operations Dashboard

Real-time flight operations control dashboard for Korean Air, powered by live data from **OpenSky Network** and **한국공항공사 (KAC) OpenAPI**.

## Architecture

```
┌───────────────────────────────────────────────────────────┐
│                      React Dashboard                       │
│          (TypeScript, Recharts, Leaflet.js, STOMP)         │
└──────────┬──────────────────┬─────────────────────────────┘
           │ REST             │ WebSocket (STOMP)
           ▼                  ▼
┌────────────────────────┐  ┌──────────────────────┐
│     Flight Service     │  │    Alert Service      │
│     (Kotlin/Spring)    │  │    (Kotlin/Spring)    │
│                        │  │                       │
│  - Flight CRUD         │  │  - Event Listener     │
│  - State Machine       │──│  - Alert Management   │
│  - OpenSky Client      │  │  - WebSocket Push     │
│  - KAC API Client      │  │                       │
│  - Statistics + Cache  │  │                       │
│  - JWT Auth + Swagger  │  │                       │
└───┬──────┬─────┬───────┘  └──────┬────────────────┘
    │      │     │                 │
    ▼      ▼     ▼                 ▼
┌──────┐ ┌─────────┐ ┌──────┐ ┌──────────────────────┐
│MySQL │ │ OpenSky │ │ KAC  │ │ MySQL                │
│flight│ │ Network │ │ API  │ │ alert_db             │
│_db   │ │ (live)  │ │(live)│ │                      │
└──────┘ └─────────┘ └──────┘ └──────────────────────┘
```

## Data Sources

This dashboard combines **three data layers** with automatic fallback:

```
Priority 1: KAC + OpenSky (combined)
  KAC  → flight number, departure/arrival airports, schedule, status
  OpenSky → real-time position, altitude, speed, heading
  = Complete flight picture

Priority 2: OpenSky only
  → aircraft positions + route mapping table

Priority 3: Mock data (demo mode)
  → 14 seeded flights + 30-second simulator
```

| Source | Data Provided | Polling | Auth |
|--------|--------------|---------|------|
| **OpenSky Network** | Aircraft position, altitude, speed, heading | 5 min | Free account (4000 calls/day) |
| **한국공항공사 (KAC)** | Flight number, airports, schedule, status (한/영) | 5 min | data.go.kr API key |
| **Mock Generator** | Full demo data with simulated events | 30 sec | None |

### OpenSky Network
- Tracks Korean Air aircraft globally via ADS-B
- ICAO operator filter: `KAL`
- Typically 30-40 aircraft tracked simultaneously
- Callsign mapping: `KAL001` → `KE001`

### 한국공항공사 OpenAPI
- Real-time flight status for KAC-managed airports
- Airports: GMP (김포), PUS (부산), CJU (제주), TAE (대구), KWJ (광주), etc.
- Note: ICN (인천) is managed by IIAC, not included in this API
- Response format: XML
- Provides: `airFln`, `boardingKor/Eng`, `arrivedKor/Eng`, `std`, `etd`, `rmkKor/Eng`

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Kotlin, Spring Boot 3.3, Spring Data JPA, Spring Security |
| Frontend | React 18, TypeScript, Recharts, Leaflet.js |
| Database | MySQL 8.0 (schema-per-service: flight_db, alert_db) |
| External APIs | OpenSky Network, 한국공항공사 OpenAPI |
| Auth | JWT (demo: admin/admin) |
| Cache | Caffeine (60s TTL) |
| Docs | SpringDoc OpenAPI / Swagger UI |
| Infra | Docker Compose, multi-stage Dockerfiles |
| Monitoring | Spring Boot Actuator |

## Quick Start

### With real data (recommended)

```bash
# Set API credentials
export OPENSKY_USERNAME=your_opensky_email
export OPENSKY_PASSWORD=your_opensky_password
export KAC_SERVICE_KEY=your_data_go_kr_key  # optional

# Run
docker-compose up --build
```

### Without API keys (demo mode)

```bash
docker-compose up --build
```

Falls back to mock data automatically. No configuration needed.

### Local development

```bash
# 1. Start MySQL
docker run -d --name korean-air-mysql \
  -e MYSQL_ROOT_PASSWORD=koreanair -p 3306:3306 \
  -v $(pwd)/docker/init.sql:/docker-entrypoint-initdb.d/init.sql mysql:8.0

# 2. Start backend services
./gradlew :alert-service:bootRun --args='--spring.profiles.active=demo' &
./gradlew :flight-service:bootRun --args='--spring.profiles.active=demo' &

# 3. Start frontend
cd frontend && npm start
```

| Service | URL |
|---------|-----|
| Dashboard | http://localhost:3000 |
| Flight API (Swagger) | http://localhost:8081/swagger-ui.html |
| Alert API (Swagger) | http://localhost:8082/swagger-ui.html |
| Health Check | http://localhost:8081/actuator/health |

### API Key Setup

**OpenSky Network** (free):
1. Register at https://opensky-network.org/register
2. Set `OPENSKY_USERNAME` and `OPENSKY_PASSWORD`

**한국공항공사** (free):
1. Register at https://data.go.kr
2. Search "한국공항공사_항공기운항정보"
3. Apply for API key (usually instant approval)
4. Set `KAC_SERVICE_KEY`

## Features

### Flight Board
- Real-time flight status from KAC API or OpenSky
- Color-coded status badges (departed, arrived, delayed, cancelled)
- Filterable by flight number, route, and status
- 3-tier data fallback: KAC+OpenSky → OpenSky → mock

### Flight Status State Machine
Valid transitions only. Invalid requests return 400.
```
SCHEDULED → BOARDING → DEPARTED → IN_FLIGHT → ARRIVED
    │            │          │           │
    └→ DELAYED ──┘          └→ DELAYED──┘
    │      │
    │      └→ BOARDING (recovery) / CANCELLED
    └→ CANCELLED
```

### Live Flight Map
- Leaflet.js map with real Korean Air aircraft positions
- OpenSky Network data with 5-minute polling
- ✈️ for airborne, 🛬 for on-ground aircraft
- Popup: callsign, altitude, speed, heading
- Green badge: "OpenSky Network (real data) — N aircraft"

### Real-time Alerts
- WebSocket (STOMP) push on status changes
- Severity: CRITICAL (delay/cancel), WARNING (gate change), INFO (status update)
- Acknowledge workflow with visual feedback (opacity 0.6)
- Yellow banner on WebSocket disconnection

### Statistics
- Departures by airport (bar chart)
- Status distribution (pie chart)
- Delay rate trend (line chart)
- Cached with Caffeine (60s TTL)

### Demo Mode
Activated via `SPRING_PROFILES_ACTIVE=demo`:
- Seeds 14 Korean Air flights across global routes
- Simulates status changes every 30 seconds via state machine
- Generates real-time alerts automatically

## API Endpoints

### Flight Service (port 8081)
```
GET    /api/flights                    - List flights (filter: airport, status)
GET    /api/flights/{id}               - Flight details
GET    /api/flights/live-tracking      - In-flight positions (mock)
GET    /api/flights/opensky            - Real KE positions from OpenSky Network
GET    /api/flights/opensky/stats      - OpenSky fleet statistics
GET    /api/flights/kac                - Real KE flights from 한국공항공사 API
GET    /api/flights/kac/stats          - KAC flight statistics
GET    /api/flights/combined           - KAC schedule + OpenSky positions merged
POST   /api/flights                    - Create flight (JWT required)
PUT    /api/flights/{id}/status        - Update status (state machine, JWT required)
PUT    /api/flights/{id}/gate          - Update gate (JWT required)
GET    /api/airports/{code}/departures - Airport departures
GET    /api/airports/{code}/arrivals   - Airport arrivals
GET    /api/stats/daily                - Daily statistics
GET    /api/stats/delay-rate           - Delay rate
POST   /api/auth/login                - Get JWT token (demo: admin/admin)
```

### Alert Service (port 8082)
```
GET    /api/alerts                     - All alerts (newest first)
GET    /api/alerts/active              - Unacknowledged alerts
PUT    /api/alerts/{id}/acknowledge    - Acknowledge alert
WS     /ws/alerts                      - Real-time alert stream (STOMP/SockJS)
```

## Project Structure

```
korean-air-ops-dashboard/
├── flight-service/
│   └── src/main/kotlin/com/koreanair/ops/flight/
│       ├── model/Flight.kt              # Entity + FlightStatus state machine
│       ├── service/FlightService.kt     # Business logic + event publishing
│       ├── service/OpenSkyClient.kt     # OpenSky Network API client
│       ├── service/KacApiClient.kt      # 한국공항공사 API client (XML)
│       ├── service/AlertNotifier.kt     # HTTP event forwarding to Alert Service
│       ├── controller/FlightController.kt  # REST + OpenSky + KAC + combined
│       ├── controller/AuthController.kt    # JWT login
│       └── config/MockFlightDataGenerator.kt  # Demo seeder + simulator
├── alert-service/
│   └── src/main/kotlin/com/koreanair/ops/alert/
│       ├── model/Alert.kt
│       ├── service/AlertService.kt      # Alert CRUD + WebSocket push
│       ├── controller/AlertController.kt
│       ├── controller/InternalEventController.kt  # Receives events from Flight Service
│       └── config/WebSocketConfig.kt    # STOMP + SockJS
├── frontend/
│   └── src/
│       ├── components/FlightBoard.tsx    # 3-tier data: combined > opensky > mock
│       ├── components/FlightMap.tsx      # Leaflet.js + OpenSky positions
│       ├── components/AlertPanel.tsx     # Real-time alerts + acknowledge
│       ├── components/StatsBar.tsx       # KPI cards (KAC > OpenSky > mock)
│       ├── components/StatsCharts.tsx    # Recharts (bar, pie, line)
│       └── hooks/useAlerts.ts           # STOMP WebSocket hook
├── docker-compose.yml                   # One-command deployment
└── docker/init.sql                      # Schema initialization
```

## Future Roadmap

- **API Gateway** (Spring Cloud Gateway) for centralized routing and rate limiting
- **Apache Kafka** for async event-driven communication (replacing HTTP callbacks)
- **Kubernetes** deployment with Helm charts
- **인천국제공항공사 API** integration for ICN airport coverage
- **ML-based delay prediction** using historical KAC data
- **Redis** for distributed caching
- **ELK Stack** for centralized logging
- **Prometheus + Grafana** for metrics dashboards
