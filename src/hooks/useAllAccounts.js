import { useQuery } from '@tanstack/react-query';
import { accountApiClient } from '../api/axiosClient';

async function fetchAllAccounts() {
  const response = await accountApiClient.get('/accounts');
  const data = response.data;
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.accounts)) return data.accounts;
  if (Array.isArray(data?.content)) return data.content;
  return [];
}

export function useAllAccounts() {
  return useQuery({
    queryKey: ['all-accounts'],
    queryFn: fetchAllAccounts
  });
}
