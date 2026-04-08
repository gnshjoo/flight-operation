# Korean Air Flight Operations Dashboard

Korean Air 실시간 운항 관제 백엔드 시스템. **OpenSky Network**과 **한국공항공사 (KAC) OpenAPI** 실시간 데이터 기반.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     API Clients                          │
│            (Swagger UI, cURL, Frontend TBD)              │
└──────────┬──────────────────┬────────────────────────────┘
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

3단계 자동 fallback 구조:

```
Priority 1: KAC + OpenSky (combined)
  KAC  → 편명, 출도착 공항, 스케줄, 상태
  OpenSky → 실시간 위치, 고도, 속도, 방위

Priority 2: OpenSky only
  → 항공기 위치 + 노선 매핑 테이블

Priority 3: Mock data (demo mode)
  → 14개 시드 항공편 + 30초 시뮬레이터
```

| Source | Data Provided | Polling | Auth |
|--------|--------------|---------|------|
| **OpenSky Network** | Aircraft position, altitude, speed, heading | 5 min | Free account |
| **한국공항공사 (KAC)** | Flight number, airports, schedule, status (한/영) | 5 min | data.go.kr API key |
| **Mock Generator** | Full demo data with simulated events | 30 sec | None |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Kotlin, Spring Boot 3.3, Spring Data JPA, Spring Security |
| Database | MySQL 8.0 (schema-per-service: flight_db, alert_db) |
| External APIs | OpenSky Network, 한국공항공사 OpenAPI |
| Auth | JWT (demo: admin/admin) |
| Cache | Caffeine (60s TTL) |
| Docs | SpringDoc OpenAPI / Swagger UI |
| Infra | Docker Compose, multi-stage Dockerfiles |
| Monitoring | Spring Boot Actuator |

## Quick Start

### 환경변수 설정

```bash
# .env 파일 생성 (.env.example 참고)
cp .env.example .env
# .env 파일에 값 입력
```

필수 환경변수:

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_PASSWORD` | MySQL root 비밀번호 | Yes |
| `JWT_SECRET` | JWT 서명 키 | Yes |
| `OPENSKY_USERNAME` | OpenSky Network 계정 | No (없으면 mock fallback) |
| `OPENSKY_PASSWORD` | OpenSky Network 비밀번호 | No |
| `KAC_SERVICE_KEY` | 한국공항공사 API 키 | No (없으면 mock fallback) |

### Docker Compose 실행

```bash
docker-compose up --build
```

API 키 없이 실행하면 자동으로 demo 모드(mock data)로 동작합니다.

### 로컬 개발

```bash
# 1. MySQL 시작
docker run -d --name flight-ops-mysql \
  -e MYSQL_ROOT_PASSWORD=yourpassword -p 3306:3306 \
  -v $(pwd)/docker/init.sql:/docker-entrypoint-initdb.d/init.sql mysql:8.0

# 2. 환경변수 설정
export DB_PASSWORD=yourpassword
export JWT_SECRET=your-secret-key

# 3. Backend 서비스 시작
./gradlew :alert-service:bootRun --args='--spring.profiles.active=demo' &
./gradlew :flight-service:bootRun --args='--spring.profiles.active=demo' &
```

| Service | URL |
|---------|-----|
| Flight API (Swagger) | http://localhost:8081/swagger-ui.html |
| Alert API (Swagger) | http://localhost:8082/swagger-ui.html |
| Health Check | http://localhost:8081/actuator/health |

### API Key 발급

**OpenSky Network** (무료):
1. https://opensky-network.org 에서 가입
2. `OPENSKY_USERNAME`, `OPENSKY_PASSWORD` 설정

**한국공항공사** (무료):
1. https://data.go.kr 에서 가입
2. "한국공항공사_항공기운항정보" 검색 후 API 키 신청
3. `KAC_SERVICE_KEY` 설정

## Features

### Flight CRUD + State Machine
- Flight 생성/조회/상태변경/게이트변경 REST API
- 상태 전이 규칙 위반 시 400 응답

```
SCHEDULED → BOARDING → DEPARTED → IN_FLIGHT → ARRIVED
    │            │          │           │
    └→ DELAYED ──┘          └→ DELAYED──┘
    │      │
    │      └→ BOARDING (recovery) / CANCELLED
    └→ CANCELLED
```

### OpenSky Network 연동
- ICAO operator filter `KAL`로 대한항공 항공기 실시간 추적
- 5분 주기 폴링, callsign 매핑 (`KAL001` → `KE001`)

### 한국공항공사 API 연동
- KAC 관할 공항(GMP, PUS, CJU, TAE, KWJ 등) 실시간 운항 정보
- XML 응답 파싱, binary search 최적화
- ICN(인천)은 IIAC 관할로 미포함

### Combined View
- KAC 스케줄 + OpenSky 위치 데이터 병합 API
- 편명 기준 매칭, 위치 보유 여부 표시

### Real-time Alerts
- Flight Service → Alert Service HTTP 이벤트 전송
- WebSocket (STOMP/SockJS) `/ws/alerts` 실시간 알림
- Severity: CRITICAL (지연/취소), WARNING (게이트 변경), INFO (상태 변경)
- Acknowledge workflow

### Statistics
- 일간 통계, 공항별 통계, 지연율
- Caffeine 캐시 (60s TTL)

### Demo Mode
`SPRING_PROFILES_ACTIVE=demo` 시 활성화:
- 14개 대한항공 글로벌 노선 시드 데이터
- 30초 주기 자동 상태 변경 시뮬레이션
- 실시간 알림 자동 생성

## API Endpoints

### Flight Service (port 8081)
```
POST   /api/auth/login                 - JWT 토큰 발급 (demo: admin/admin)

GET    /api/flights                     - 항공편 목록 (filter: airport, status)
GET    /api/flights/{id}                - 항공편 상세
POST   /api/flights                     - 항공편 생성 (JWT)
PUT    /api/flights/{id}/status         - 상태 변경 (state machine, JWT)
PUT    /api/flights/{id}/gate           - 게이트 변경 (JWT)

GET    /api/flights/live-tracking       - 비행 중 항공기 위치 (mock)
GET    /api/flights/opensky             - OpenSky 실시간 위치
GET    /api/flights/opensky/stats       - OpenSky 통계
GET    /api/flights/kac                 - KAC 실시간 운항 정보
GET    /api/flights/kac/stats           - KAC 통계
GET    /api/flights/combined            - KAC + OpenSky 병합 데이터

GET    /api/airports/{code}/departures  - 공항별 출발 편
GET    /api/airports/{code}/arrivals    - 공항별 도착 편

GET    /api/stats/daily                 - 일간 통계
GET    /api/stats/airport/{code}        - 공항별 통계
GET    /api/stats/delay-rate            - 지연율
```

### Alert Service (port 8082)
```
GET    /api/alerts                      - 전체 알림 (최신순)
GET    /api/alerts/active               - 미확인 알림
PUT    /api/alerts/{id}/acknowledge     - 알림 확인 처리

POST   /api/internal/events/status-changed  - 상태 변경 이벤트 (내부)
POST   /api/internal/events/gate-changed    - 게이트 변경 이벤트 (내부)

WS     /ws/alerts                       - 실시간 알림 스트림 (STOMP/SockJS)
```

## Project Structure

```
flight-operation/
├── flight-service/
│   └── src/main/kotlin/com/koreanair/ops/flight/
│       ├── model/Flight.kt                    # Entity + FlightStatus state machine
│       ├── service/FlightService.kt           # Business logic + event publishing
│       ├── service/FlightRepository.kt        # JPA repository
│       ├── service/OpenSkyClient.kt           # OpenSky Network API client
│       ├── service/KacApiClient.kt            # 한국공항공사 API client (XML)
│       ├── service/AlertNotifier.kt           # HTTP event forwarding to Alert Service
│       ├── controller/FlightController.kt     # Flight + OpenSky + KAC + combined
│       ├── controller/AuthController.kt       # JWT login
│       ├── dto/FlightDtos.kt                  # Request/Response DTOs
│       ├── event/FlightEvents.kt              # Status/gate change events
│       └── config/
│           ├── MockFlightDataGenerator.kt     # Demo seeder + simulator
│           ├── CacheConfig.kt                 # Caffeine cache
│           ├── SecurityConfig.kt              # JWT + Spring Security
│           └── GlobalExceptionHandler.kt      # Error handling
├── alert-service/
│   └── src/main/kotlin/com/koreanair/ops/alert/
│       ├── model/Alert.kt                     # Alert entity + enums
│       ├── service/AlertService.kt            # Alert CRUD + WebSocket push
│       ├── service/AlertRepository.kt         # JPA repository
│       ├── controller/AlertController.kt      # REST endpoints
│       ├── controller/InternalEventController.kt  # Flight Service event receiver
│       ├── dto/AlertDtos.kt                   # Request/Response DTOs
│       └── config/
│           ├── WebSocketConfig.kt             # STOMP + SockJS
│           ├── SecurityConfig.kt              # Spring Security
│           └── GlobalExceptionHandler.kt      # Error handling
├── docker-compose.yml                         # MySQL + services orchestration
├── docker/init.sql                            # Database initialization
└── .env.example                               # 환경변수 템플릿
```
