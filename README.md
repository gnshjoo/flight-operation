# Korean Air Flight Operations Dashboard

Real-time flight operations control dashboard for Korean Air, built with Kotlin/Spring Boot microservices and React.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    React Dashboard                   │
│         (TypeScript, Recharts, Leaflet.js)           │
└──────────┬──────────────────┬───────────────────────┘
           │ REST             │ WebSocket (STOMP)
           ▼                  ▼
┌──────────────────┐  ┌──────────────────────┐
│  Flight Service  │  │   Alert Service      │
│  (Kotlin/Spring) │  │   (Kotlin/Spring)    │
│                  │  │                      │
│ - Flight CRUD    │  │ - Event Listener     │
│ - State Machine  │──│ - Alert Management   │
│ - Statistics     │  │ - WebSocket Push     │
│ - JWT Auth       │  │                      │
│ - Swagger UI     │  │                      │
└──────┬───────────┘  └──────┬───────────────┘
       │ ApplicationEvent    │
       ▼                     ▼
  ┌──────────────────────────────┐
  │ MySQL (flight_db + alert_db) │
  └──────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Kotlin, Spring Boot 3.3, Spring Data JPA, Spring Security |
| Frontend | React 18, TypeScript, Recharts, Leaflet.js |
| Database | MySQL 8.0 (schema-per-service) |
| Auth | JWT (demo: admin/admin) |
| Cache | Caffeine (60s TTL for stats) |
| Docs | SpringDoc OpenAPI / Swagger UI |
| Infra | Docker Compose, multi-stage builds |
| Monitoring | Spring Boot Actuator |

## Quick Start

```bash
docker-compose up --build
```

| Service | URL |
|---------|-----|
| Dashboard | http://localhost:3000 |
| Flight API | http://localhost:8081/swagger-ui.html |
| Alert API | http://localhost:8082/swagger-ui.html |
| Flight Health | http://localhost:8081/actuator/health |

## Features

### Flight Board
- Real-time flight status with color-coded badges
- Filterable by airport and status
- Sortable columns

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

### Real-time Alerts
- WebSocket (STOMP) push on status changes
- Severity levels: CRITICAL, WARNING, INFO
- Acknowledge workflow with visual feedback

### Live Flight Map
- Leaflet.js map with aircraft positions
- 30-second polling interval
- Popup with flight details

### Statistics
- Daily flight counts (bar chart)
- Status distribution (pie chart)
- Delay rate trend (line chart)
- Cached with Caffeine (60s TTL)

### Demo Mode
Activated via `SPRING_PROFILES_ACTIVE=demo`:
- Seeds 14 Korean Air flights across global routes
- Simulates status changes every 30 seconds
- Generates real-time alerts automatically

## API Endpoints

### Flight Service (port 8081)
```
GET    /api/flights                    - List flights (filter: airport, status)
GET    /api/flights/{id}               - Flight details
GET    /api/flights/live-tracking      - In-flight positions
POST   /api/flights                    - Create flight (JWT required)
PUT    /api/flights/{id}/status        - Update status (state machine validated, JWT required)
PUT    /api/flights/{id}/gate          - Update gate (JWT required)
GET    /api/airports/{code}/departures - Airport departures
GET    /api/airports/{code}/arrivals   - Airport arrivals
GET    /api/stats/daily                - Daily statistics
GET    /api/stats/delay-rate           - Delay rate
POST   /api/auth/login                - Get JWT token (admin/admin)
```

### Alert Service (port 8082)
```
GET    /api/alerts                     - All alerts
GET    /api/alerts/active              - Unacknowledged alerts
PUT    /api/alerts/{id}/acknowledge    - Acknowledge alert
WS     /ws/alerts                      - Real-time alert stream (STOMP)
```

## Project Structure

```
korean-air-ops-dashboard/
├── flight-service/          # Flight management microservice
├── alert-service/           # Alert & WebSocket microservice
├── frontend/                # React dashboard
├── docker/                  # DB init scripts
├── docker-compose.yml       # One-command deployment
└── README.md
```

## Future Roadmap

In a production environment, this system would be extended with:

- **API Gateway** (Spring Cloud Gateway) for centralized routing, rate limiting, and auth
- **Apache Kafka** for async event-driven communication between services (replacing ApplicationEvent)
- **Kubernetes** deployment with Helm charts for scaling and resilience
- **OpenSky Network API** integration for real flight position data
- **ML-based delay prediction** using historical data patterns
- **Redis** for distributed caching across service instances
- **ELK Stack** (Elasticsearch, Logstash, Kibana) for centralized logging
- **Prometheus + Grafana** for metrics and alerting dashboards
