import { loginApiClient } from './axiosClient';

export async function registerUser(payload) {
  const response = await loginApiClient.post('/api/auth/register', payload);
  return response.data;
}

export async function loginUser(payload) {
  const response = await loginApiClient.post('/api/auth/login', payload);
  return response.data;
}