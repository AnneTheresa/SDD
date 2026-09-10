import { useMutation } from '@tanstack/react-query';
import { deleteCustomer } from '../api/accounts';

export function useDeleteCustomer() {
  return useMutation({
    mutationFn: deleteCustomer,
    throwOnError: false
  });
}
