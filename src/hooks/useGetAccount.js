import { useQuery } from '@tanstack/react-query';
import { getAccount } from '../api/accounts';

export function useGetAccount(accountId) {
  return useQuery({
    queryKey: ['account', accountId],
    queryFn: () => getAccount(accountId),
    enabled: Boolean(accountId)
  });
}
