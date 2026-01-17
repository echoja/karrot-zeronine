import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * 주문 생성 부하 테스트
 *
 * 시나리오: 인기 딜에 100명이 동시에 주문하는 상황
 * - Redis Lua Script 재고 차감 성능 검증
 * - 분산 락 동시성 제어 검증
 */
export const options = {
  scenarios: {
    // 시나리오 1: 점진적 부하 증가
    gradual_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 20 },
        { duration: '30s', target: 100 },
        { duration: '10s', target: 100 },
        { duration: '10s', target: 0 },
      ],
      exec: 'orderTest',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],  // 95% 요청이 1초 이내
    http_req_failed: ['rate<0.05'],     // 실패율 5% 미만 (재고 소진 허용)
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DEAL_UUID = __ENV.DEAL_UUID || '';

export function setup() {
  // 테스트용 딜 생성
  if (!DEAL_UUID) {
    const payload = JSON.stringify({
      title: '부하테스트 주문용 딜',
      description: '동시성 테스트',
      originalPrice: 10000,
      dealPrice: 7000,
      totalStock: 1000,  // 충분한 재고
      startTime: new Date(Date.now() - 3600000).toISOString().slice(0, 19),
      endTime: new Date(Date.now() + 86400000).toISOString().slice(0, 19),
      regionCode: 'SEOUL-01',
    });

    const headers = {
      'Content-Type': 'application/json',
      'X-User-Id': '1',
    };

    const createResponse = http.post(`${BASE_URL}/api/v1/deals`, payload, { headers });
    const dealData = JSON.parse(createResponse.body);

    if (!dealData.success) {
      console.error('딜 생성 실패:', createResponse.body);
      return { dealUuid: null };
    }

    const dealUuid = dealData.data.uuid;

    // 딜 활성화
    http.post(`${BASE_URL}/api/v1/deals/${dealUuid}/activate`, null, { headers });

    console.log(`테스트 딜 생성 완료: ${dealUuid}`);
    return { dealUuid };
  }

  return { dealUuid: DEAL_UUID };
}

export function orderTest(data) {
  if (!data.dealUuid) {
    console.error('딜 UUID가 없습니다');
    return;
  }

  const buyerId = Math.floor(Math.random() * 100000) + 1;

  const payload = JSON.stringify({
    dealUuid: data.dealUuid,
    quantity: 1,
  });

  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': `${buyerId}`,
  };

  const response = http.post(`${BASE_URL}/api/v1/orders`, payload, { headers });

  check(response, {
    'status is 201 or 400': (r) => r.status === 201 || r.status === 400,
    'response has body': (r) => r.body.length > 0,
  });

  sleep(0.05);
}

export default function (data) {
  orderTest(data);
}
