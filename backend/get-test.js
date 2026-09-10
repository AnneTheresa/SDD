import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '30s',
};

function loginAndGetToken() {
  const res = http.post('http://localhost:8080/api/auth/login', JSON.stringify({
    username: 'admin@fdmgroup.com',
    password: 'Secure@123',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  return JSON.parse(res.body).accessToken;
}

export default function () {
  const token = loginAndGetToken();

  const res = http.get('http://localhost:8080/api/customers/1', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}