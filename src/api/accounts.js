import { createIdempotencyKey } from '../types';
import { accountApiClient } from './axiosClient';

function buildIdempotencyHeaders(idempotencyKey) {
  return {
    headers: {
      'Idempotency-Key': idempotencyKey || createIdempotencyKey()
    }
  };
}

export async function getInterestRates() {
  const response = await accountApiClient.get('/api/interest-rates');
  return response.data;
}

export async function createAccount(payload) {
  const body = {
    accountType: payload.accountType,
    balance: parseFloat(payload.balance)
  };

  if (payload.accountType === 'RRSP') {
    body.balance = 0;
    body.interestRate = 0.5;
  } else if (payload.accountType === 'SAVINGS' || payload.accountType === 'TFSA') {
    body.interestRate = parseFloat(payload.interestRate);
  }

  if (payload.accountType === 'TFSA' && payload.dateOfBirth) {
    body.dateOfBirth = payload.dateOfBirth;
  }

  const response = await accountApiClient.post(`/customers/${payload.customerId}/accounts`, body);
  return response.data;
}

export async function getAccount(accountId) {
  const response = await accountApiClient.get(`/accounts/${accountId}`);
  return response.data;
}

export async function listCustomerAccounts(customerId) {
  const response = await accountApiClient.get(`/customers/${customerId}/accounts`);
  const data = response.data;
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.accounts)) return data.accounts;
  if (Array.isArray(data?.content)) return data.content;
  return [];
}

export async function updateAccount(payload) {
  const body = {};

  if (payload.interestRate !== '') {
    body.interestRate = payload.interestRate;
  }

  const response = await accountApiClient.put(`/accounts/${payload.accountId}`, body);
  return response.data;
}

export async function adminUpdateInterestRate(accountId, interestRate) {
  const response = await accountApiClient.put(`/accounts/${accountId}`, {
    interestRate,
    empty: true
  });
  return response.data;
}

export async function deleteAccount(accountId) {
  const response = await accountApiClient.delete(`/accounts/${accountId}`);
  return response.data;
}

export async function deleteCustomer(customerId) {
  const response = await accountApiClient.delete(`/api/customers/${customerId}`);
  return response.data;
}

export async function depositToAccount(payload) {
  const response = await accountApiClient.post(
    `/accounts/${payload.accountId}/deposit`,
    {
      amount: payload.amount,
      description: payload.description || null,
      category: payload.category || null
    },
    buildIdempotencyHeaders(payload.idempotencyKey)
  );
  return response.data;
}

export async function withdrawFromAccount(payload) {
  const response = await accountApiClient.post(
    `/accounts/${payload.accountId}/withdraw`,
    {
      amount: payload.amount,
      description: payload.description || null,
      category: payload.category || null
    },
    buildIdempotencyHeaders(payload.idempotencyKey)
  );
  return response.data;
}

export async function getGics(accountId) {
  const response = await accountApiClient.get(`/accounts/${accountId}/gic`);
  return response.data;
}

export async function openGic(accountId, payload) {
  const response = await accountApiClient.post(`/accounts/${accountId}/gic`, {
    amount: payload.amount,
    term: payload.term
  });
  return response.data;
}

export async function redeemGic(accountId, gicId) {
  const response = await accountApiClient.post(`/accounts/${accountId}/gic/${gicId}/redeem`);
  return response.data;
}

export async function closeRrspAccount(accountId) {
  const response = await accountApiClient.post(`/accounts/${accountId}/close`);
  return response.data;
}

export async function transferBetweenAccounts(payload) {
  const response = await accountApiClient.post(
    '/accounts/transfer',
    {
      fromAccountId: payload.fromAccountId,
      toAccountId: payload.toAccountId,
      amount: payload.amount,
      description: payload.description || null,
      category: payload.category || null
    },
    buildIdempotencyHeaders(payload.idempotencyKey)
  );
  return response.data;
}

export async function freezeAccount(payload) {
  const response = await accountApiClient.post(`/accounts/${payload.accountId}/freeze`, {
    reason: payload.reason,
    reasonCode: payload.reasonCode || null,
    notes: payload.notes || null
  });
  return response.data;
}

export async function unfreezeAccount(payload) {
  const response = await accountApiClient.post(`/accounts/${payload.accountId}/unfreeze`, {
    reason: payload.reason || null,
    notes: payload.notes || null
  });
  return response.data;
}

export async function getAccountControlHistory(accountId) {
  const response = await accountApiClient.get(`/accounts/${accountId}/control-history`);
  return response.data;
}
