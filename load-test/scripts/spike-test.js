import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

/**
 * 스파이크 테스트
 *
 * 시나리오: 타임딜 시작 시 갑자기 트래픽이 폭증하는 상황
 * - 순간적으로 500명이 동시 접속
 * - 시스템 복원력 검증
 */
export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 10 },    // 워밍업
        { duration: '5s', target: 500 },   // 스파이크!
        { duration: '10s', target: 500 },  // 유지
        { duration: '5s', target: 10 },    // 감소
        { duration: '5s', target: 0 },     // 종료
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // 스파이크 시 2초 허용
    http_req_failed: ['rate<0.10'],     // 실패율 10% 미만
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // 딜 목록 조회 (읽기 부하)
  const listResponse = http.get(`${BASE_URL}/api/v1/deals/region/SEOUL-01`);

  check(listResponse, {
    'list status is 200': (r) => r.status === 200,
  });

  sleep(0.01);
}
