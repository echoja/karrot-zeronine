import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  scenarios: {
    deal_create: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 10 },  // 10초간 10명까지 증가
        { duration: '20s', target: 50 },  // 20초간 50명까지 증가
        { duration: '10s', target: 0 },   // 10초간 0명으로 감소
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% 요청이 500ms 이내
    http_req_failed: ['rate<0.01'],    // 실패율 1% 미만
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const payload = JSON.stringify({
    title: `부하테스트 딜 ${randomString(8)}`,
    description: '부하 테스트용 딜입니다',
    originalPrice: 10000,
    dealPrice: 7000,
    totalStock: 100,
    startTime: new Date(Date.now() + 3600000).toISOString().slice(0, 19),
    endTime: new Date(Date.now() + 86400000).toISOString().slice(0, 19),
    regionCode: 'SEOUL-01',
  });

  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': `${Math.floor(Math.random() * 1000) + 1}`,
  };

  const response = http.post(`${BASE_URL}/api/v1/deals`, payload, { headers });

  check(response, {
    'status is 201': (r) => r.status === 201,
    'success is true': (r) => JSON.parse(r.body).success === true,
  });

  sleep(0.1);
}
