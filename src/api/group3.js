import { accountApiClient } from './axiosClient';

function toLocalDateTime(value) {
  if (!value) {
    return value;
  }

  // Backend expects LocalDateTime without timezone designator.
  // If date-only (10 chars: YYYY-MM-DD), append T00:00:00
  // If datetime-local (16 chars: YYYY-MM-DDTHH:MM), append :00
  if (value.length === 10) {
    return `${value}T00:00:00`;
  }
  return value.length === 16 ? `${value}:00` : value;
}

function splitPeriod(period) {
  const [year, month] = String(period || '').split('-');
  return { year, month };
}

function parseJsonFromArrayBuffer(buffer) {
  if (!(buffer instanceof ArrayBuffer)) {
    return null;
  }

  try {
    const text = new TextDecoder('utf-8').decode(buffer).trim();
    if (!text) {
      return null;
    }

    return JSON.parse(text);
  } catch {
    return null;
  }
}

export async function getTransactionHistory({ accountId, startDate, endDate }) {
  const response = await accountApiClient.get(`/accounts/${accountId}/transactions`, {
    params: {
      startDate: startDate || undefined,
      endDate: endDate || undefined
    }
  });

  return response.data;
}

export async function exportTransactionHistoryPdf({ accountId, startDate, endDate }) {
  const response = await accountApiClient.get(`/accounts/${accountId}/transactions/export`, {
    params: {
      startDate: startDate || undefined,
      endDate: endDate || undefined
    },
    responseType: 'blob'
  });

  return response.data;
}

export async function createStandingOrder(payload) {
  const response = await accountApiClient.post(`/accounts/${payload.accountId}/standing-orders`, {
    payeeAccount: payload.payeeAccount,
    payeeName: payload.payeeName,
    amount: payload.amount,
    frequency: payload.frequency,
    startDate: toLocalDateTime(payload.startDate),
    endDate: payload.endDate ? toLocalDateTime(payload.endDate) : null,
    reference: payload.reference,
    category: payload.category || null
  });

  return response.data;
}

export async function listStandingOrders(accountId) {
  const response = await accountApiClient.get(`/accounts/${accountId}/standing-orders`);
  return response.data;
}

export async function cancelStandingOrder(standingOrderId) {
  const response = await accountApiClient.delete(`/standing-orders/${standingOrderId}`);
  return response.data;
}

export async function getMonthlyStatement({ accountId, period }) {
  try {
    const response = await accountApiClient.get(`/accounts/${accountId}/statements/${period}`, {
      responseType: 'arraybuffer'
    });

    return new Blob([response.data], {
      type: response.headers?.['content-type'] || 'application/pdf'
    });
  } catch (error) {
    const parsedErrorBody = parseJsonFromArrayBuffer(error?.response?.data);
    if (parsedErrorBody && error?.response) {
      error.response.data = parsedErrorBody;
    }
    throw error;
  }
}

export async function getSpendingInsights({ accountId, period }) {
  const { year, month } = splitPeriod(period);
  const response = await accountApiClient.get(`/accounts/${accountId}/insights`, {
    params: {
      year,
      month
    }
  });

  return response.data;
}

export async function recategoriseTransaction({ accountId, transactionId, category }) {
  const response = await accountApiClient.put(
    `/accounts/${accountId}/transactions/${transactionId}/category`,
    { category }
  );

  return response.data;
}