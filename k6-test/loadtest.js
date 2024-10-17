import http from 'k6/http';
import {check, sleep} from 'k6';

export let options = {
  stages: [
    {duration: '30s', target: 50},  // 30초 동안 VU(User) 50까지 증가
    {duration: '1m', target: 100},  // 1분 동안 VU 100 유지
    {duration: '30s', target: 0},   // 30초 동안 VU 0으로 감소
  ],
};

export default function () {
  const tripId = Math.floor(Math.random() * 1000) + 1;  // 임의의 tripId 생성 (1~1000 범위)

  let res = http.get(`http://your-ec2-public-ip/api/trips/${tripId}`, {
    tags: {name: 'GetTripInfo'}  // 태그 추가
  });

  check(res, {
    'status was 200': (r) => r.status === 200,
    'response is not empty': (r) => r.body.length > 0,
  });

  sleep(1);  // 각 요청마다 1초 대기
}
