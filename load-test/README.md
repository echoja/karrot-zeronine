# 부하 테스트 (k6)

k6를 사용한 부하 테스트 스크립트입니다.

## 설치

```bash
# macOS
brew install k6

# 또는 Docker
docker pull grafana/k6
```

## 테스트 스크립트

| 스크립트 | 설명 | 시나리오 |
|----------|------|----------|
| `deal-create.js` | 딜 생성 API 테스트 | 점진적 부하 (10→50 VUs) |
| `order-create.js` | 주문 생성 API 테스트 | 동시성 테스트 (100 VUs) |
| `spike-test.js` | 스파이크 테스트 | 순간 폭증 (500 VUs) |

## 실행 방법

### 1. 서버 실행

```bash
docker-compose up -d
./gradlew :api:bootRun
```

### 2. 테스트 실행

```bash
# 딜 생성 부하 테스트
k6 run load-test/scripts/deal-create.js

# 주문 생성 부하 테스트 (동시성 검증)
k6 run load-test/scripts/order-create.js

# 스파이크 테스트
k6 run load-test/scripts/spike-test.js

# 환경변수로 서버 URL 지정
k6 run -e BASE_URL=http://localhost:8080 load-test/scripts/deal-create.js
```

### 3. Docker로 실행

```bash
docker run --rm -i --network=host grafana/k6 run - < load-test/scripts/deal-create.js
```

## 결과 지표

| 지표 | 설명 | 목표 |
|------|------|------|
| `http_req_duration` | 요청 응답 시간 | p95 < 500ms |
| `http_req_failed` | 실패율 | < 1% |
| `vus` | 동시 가상 사용자 | - |
| `iterations` | 총 요청 수 | - |

## 결과 예시

```
     ✓ status is 201
     ✓ success is true

     checks.........................: 100.00% ✓ 2000  ✗ 0
     http_req_duration..............: avg=45ms  min=12ms  max=234ms  p(95)=120ms
     http_req_failed................: 0.00%   ✓ 0     ✗ 1000
     http_reqs......................: 1000    50/s
     vus............................: 50      min=0   max=50
```

## HTML 리포트 생성

```bash
# k6 Cloud 또는 InfluxDB + Grafana 연동 권장
k6 run --out json=result.json load-test/scripts/deal-create.js

# JSON 결과를 HTML로 변환 (k6-reporter 사용)
npx k6-reporter result.json
```
