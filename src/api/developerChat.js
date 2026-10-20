import { accountApiClient } from './axiosClient';

export async function askDeveloperChat({ message, sessionId }) {
  const response = await accountApiClient.post('/api/developer/chat', {
    message,
    sessionId: sessionId || null,
  });
  return response.data;
}
