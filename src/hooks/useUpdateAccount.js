import { useMutation } from '@tanstack/react-query';
import { updateAccount } from '../api/accounts';

export function useUpdateAccount() {
  return useMutation({
    mutationFn: updateAccount,
    throwOnError: false
  });
}
