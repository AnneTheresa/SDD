import { useMutation } from '@tanstack/react-query';
import { deleteAccount } from '../api/accounts';

export function useDeleteAccount() {
  return useMutation({
    mutationFn: deleteAccount,
    throwOnError: false
  });
}
