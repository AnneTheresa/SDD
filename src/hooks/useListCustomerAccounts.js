import { useQuery } from '@tanstack/react-query';
import { listCustomerAccounts } from '../api/accounts';

export function useListCustomerAccounts(customerId) {
  return useQuery({
    queryKey: ['customer-accounts', customerId],
    queryFn: () => listCustomerAccounts(customerId),
    enabled: Boolean(customerId)
  });
}
