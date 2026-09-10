import { useMutation } from '@tanstack/react-query';
import { withdrawFromAccount } from '../api/accounts';

export function useWithdraw() {
  return useMutation({
    mutationFn: withdrawFromAccount,
    throwOnError: false
  });
}
