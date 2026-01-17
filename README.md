# karrot-zeronine

우리동네 타임딜, "당근공구": 동네 이웃과 함께하는 타임딜 공동구매 플랫폼

> Kotlin + Spring Boot + Kafka 실전 학습 가즈아

---

## 🚀 구현 현황

### Tech Stack
| Category | Technology | Version |
|----------|-----------|---------|
| Language | Kotlin | 2.3.0 |
| Framework | Spring Boot | 3.4.1 |
| JDK | Java | 21 LTS |
| Database | PostgreSQL | 17 |
| Cache | Redis | 7 |
| Message Queue | Kafka | 7.9 |
| Testing | Kotest + MockK | 6.0.7 |

### 프로젝트 구조 (멀티 모듈)
```
karrot-zeronine/
├── common/     # 확장 함수, 유틸리티
├── core/       # 도메인 모델, Redis, Kafka, 캐싱
├── api/        # REST API, Swagger, Coroutines
├── batch/      # 배치 작업, Kafka Consumer
└── docker-compose.yml
```

### ✅ 구현 완료 기능

#### A. Kotlin Idiomatic Programming
- [x] **Null Safety**: `?`, `?:` 연산자 활용, `Result<T>` 확장 함수
- [x] **Extension Function**: `Deal.toResponse()`, `Order.toResponse()` DTO 변환
- [x] **DSL**: `TestDataDsl` - Type-safe builder 패턴 테스트 데이터 생성
- [x] **Immutability**: `val` 최대 활용, 불변 객체 설계

#### B. 동시성 제어 및 대규모 트래픽 대응
- [x] **Redis Lua Script**: `StockRedisService` - 원자적 재고 차감
- [x] **Distributed Lock**: `DistributedLockService` - Redisson 분산 락
- [x] **Pessimistic Lock**: `DealRepository.findByIdWithPessimisticLock()`
- [x] **Optimistic Lock**: `BaseEntityWithVersion` (@Version 필드)
- [x] **Cache Layer**: `DealCacheService` - Cache-Aside 패턴, 인기 딜 Sorted Set

#### C. 비동기 이벤트 기반 아키텍처
- [x] **Kafka Events**: `OrderEvent`, `DealEvent` sealed class
- [x] **Event Publisher**: 주문/딜 이벤트 발행
- [x] **Event Consumer**: 배치 모듈 Kafka Consumer
- [x] **Kotlin Coroutines**: `DealAsyncService` - 병렬 조회 (async/await)

#### D. 테스트
- [x] **Kotest**: DescribeSpec, BehaviorSpec, FunSpec 스타일
- [x] **MockK**: 의존성 모킹
- [x] **Testcontainers**: PostgreSQL, Redis 통합 테스트
- [x] **Test DSL**: `deal { }`, `order { }` 빌더 패턴

---

## 🏃 Quick Start

### 1. 인프라 실행
```bash
# PostgreSQL, Redis, Kafka 컨테이너 실행
docker-compose up -d
```

### 2. 빌드 & 실행
```bash
# 빌드
./gradlew build

# API 서버 실행
./gradlew :api:bootRun

# 배치 서버 실행 (별도 터미널)
./gradlew :batch:bootRun
```

### 3. API 테스트
```bash
# Swagger UI
open http://localhost:8080/swagger-ui.html

# 딜 생성
curl -X POST http://localhost:8080/api/v1/deals \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "title": "테스트 딜",
    "description": "설명",
    "originalPrice": 10000,
    "dealPrice": 7000,
    "totalStock": 100,
    "startTime": "2025-01-18T10:00:00",
    "endTime": "2025-01-19T10:00:00",
    "regionCode": "SEOUL-01"
  }'
```

### 4. 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :core:test
./gradlew :api:test
```

---

## 📊 주요 API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/deals` | 딜 생성 |
| GET | `/api/v1/deals/{uuid}` | 딜 조회 |
| POST | `/api/v1/deals/{uuid}/activate` | 딜 활성화 |
| GET | `/api/v1/deals/region/{code}` | 지역별 딜 목록 |
| POST | `/api/v1/orders` | 주문 생성 |
| POST | `/api/v1/orders/{uuid}/pay` | 결제 완료 |
| POST | `/api/v1/orders/{uuid}/cancel` | 주문 취소 |
| GET | `/api/v1/deals/async/regions` | 다중 지역 병렬 조회 (Coroutines) |

---


## 핵심 기술 목표 (Kotlin & Spring Boot 마스터링 포인트)

이 프로젝트는 단순한 CRUD가 아니라, **시니어 개발자로서의 고민**을 코드에 녹여내는 것이 핵심입니다.

### A. Kotlin Idiomatic Programming (코틀린답게 짜기)

* **목표:** Java 코드를 문법만 바꾼 것이 아니라, Kotlin의 강점을 100% 활용합니다.
* **구현 과제:**
* **Null Safety:** `Optional` 대신 Kotlin의 Nullable 타입(`?`)과 Elvis 연산자(`?:`)를 사용하여 우아하게 예외를 처리합니다.
* **Extension Function:** 엔티티와 DTO 간 변환 로직을 확장 함수로 분리하여 도메인 모델의 순수성을 지킵니다.
* **DSL (Domain Specific Language):** 복잡한 검색 조건이나 테스트 데이터 생성을 위해 `Type-safe builder` 패턴을 직접 구현해 봅니다.
* **Immutability:** 모든 변수를 `val`로 선언하고 불변 객체 설계를 지향하여 사이드 이펙트를 최소화합니다.

### B. 동시성 제어 및 대규모 트래픽 대응 (JD의 '트래픽 폭증' 해결)**

* **시나리오:** 인기 상품이 100개 한정으로 풀릴 때, 1만 명이 동시에 구매 버튼을 누르는 상황.
* **구현 과제:**
* **Redis Lua Script:** 재고 차감 연산의 원자성(Atomicity)을 보장하기 위해 Redis 레벨에서 제어합니다.
* **Distributed Lock (Redisson):** 분산 환경에서 다중 서버가 동시에 DB에 접근할 때 발생하는 Race Condition을 해결합니다.
* **DB Lock:** 비관적 락(Pessimistic Lock)과 낙관적 락(Optimistic Lock)의 성능 차이를 직접 벤치마킹하고 상황에 맞는 전략을 선택합니다.

### C. 비동기 이벤트 기반 아키텍처 (JD의 '확장성' 및 '시스템 연동')

* **시나리오:** 주문 완료 시 '배송 요청', '알림 발송', '통계 집계'가 동시에 일어나야 함.
* **구현 과제:**
* **Kafka/RabbitMQ:** 결제 완료 이벤트를 발행하고, 결제 서비스와 주문/배송 서비스 간의 결합도를 낮춥니다(Loose Coupling).
* **Kotlin Coroutines:** I/O 작업(DB 조회, 외부 API 호출)이 많은 로직을 Non-blocking으로 처리하여 처리량(Throughput)을 극대화합니다.

## 2. 추천 시스템 아키텍처 (Tech Stack)

* **Language:** Kotlin 2.3+
* **Framework:** Spring Boot 3.2+ (Java 21 LTS 기반)
* **Database:**
* **Main:** Postgres (Master/Slave 리플리케이션 구성으로 읽기/쓰기 부하 분산)
* **Cache:** Redis (세션 관리, 인기 상품 캐싱, 재고 관리)


* **Message Queue:** Kafka (주문 이벤트 처리)
* **Testing:** Kotest + MockK (JUnit/Mockito 대신 Kotlin 전용 라이브러리 사용 필수)

#### 3. 당근마켓 합격을 위한 '플러스 알파' 전략

JD의 "AI 활용"과 "오너십" 항목을 공략하기 위한 전략입니다.

1. **AI 도구 적극 활용의 흔적 남기기:**
* 단순 구현에 그치지 말고, **Claude Code나 Cursor**를 활용하여 테스트 커버리지를 90% 이상으로 높인 과정, 혹은 복잡한 레거시 코드를 리팩토링한 과정을 PR(Pull Request) 설명이나 기술 블로그에 기록하세요. (예: "Cursor를 활용해 반복적인 DTO 매핑 코드를 자동화하여 생산성 30% 향상")


2. **성능 테스트 보고서 작성 (가장 중요):**
* **nGrinder**나 **JMeter**를 사용하여 트래픽을 쏘아보고, 시스템이 어디서 병목(Bottle neck)이 걸리는지 분석한 리포트를 프로젝트 README에 포함하세요.
* *예: "초당 500건 주문 요청 시 DB CPU가 튀는 현상을 발견하여 Redis 캐싱 전략을 도입, TPS를 3배 개선함"* -> 이 한 줄이 합격을 결정짓습니다.


3. **멀티 모듈 (Multi-module) 설계:**
* 프로젝트를 `core`, `api`, `batch`, `common` 등으로 나누어, 대규모 프로젝트에서도 빌드 속도와 의존성을 관리할 수 있는 능력을 보여주세요.



이 프로젝트를 통해 "Kotlin 문법을 안다"는 수준을 넘어, **"Kotlin과 Spring Boot로 대규모 시스템의 문제를 해결할 수 있다"**는 것을 증명하신다면 당근마켓 커머스팀에 충분히 어필할 수 있을 것입니다.