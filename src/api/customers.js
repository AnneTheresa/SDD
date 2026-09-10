import { loginApiClient } from './axiosClient';

export async function createCustomer(payload, accessToken) {
  const response = await loginApiClient.post('/api/customers', payload, accessToken ? {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  } : undefined);
  return response.data;
}

export async function listCustomers() {
  const response = await loginApiClient.get('/api/customers');
  return response.data;
}

export async function getCustomer(customerId) {
  const response = await loginApiClient.get(`/api/customers/${customerId}`);
  return response.data;
}

export async function updateCustomer(customerId, payload) {
  const response = await loginApiClient.patch(`/api/customers/${customerId}`, payload);
  return response.data;
}