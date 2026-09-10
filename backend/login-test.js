import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '30s',
};

export default function () {
  const payload = JSON.stringify({
    username: 'admin@fdmgroup.com',
    password: 'Secure@123',
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const res = http.post('http://localhost:8080/api/auth/login', payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has access token': (r) => r.body.includes('accessToken'),
  });

  sleep(1);
}