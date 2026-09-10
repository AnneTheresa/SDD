import { accountApiClient } from './axiosClient';

/**
 * Savings Insight Chatbot (BRD 6.1). The backend agent decides for itself which
 * customer data (accounts, spending, savings goals) or knowledge base articles to
 * pull in before answering — this call just sends the raw message and gets back a
 * ready-to-render response, its basis, and whether it's a blocked/limited-data reply.
 */
export async function askSavingsInsight(message) {
  const response = await accountApiClient.post('/api/chat/savings-insights', { message });
  return response.data;
}

/**
 * Confirms (and executes) a pending agent-proposed action, e.g. a transfer the
 * chatbot prepared but did not carry out (AC4). The model never has a path to this
 * endpoint - only an explicit customer action in the chat UI calls it.
 */
export async function confirmAgentAction(token) {
  const response = await accountApiClient.post(`/api/chat/confirmations/${token}`);
  return response.data;
}
