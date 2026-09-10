import { useMutation } from '@tanstack/react-query';
import { depositToAccount } from '../api/accounts';

export function useDeposit() {
  return useMutation({
    mutationFn: depositToAccount,
    throwOnError: false
  });
}
