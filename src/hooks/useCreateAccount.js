import { useMutation } from '@tanstack/react-query';
import { createAccount } from '../api/accounts';
import { mapAxiosError } from '../api/axiosClient';

export function useCreateAccount() {
  return useMutation({
    mutationFn: createAccount,
    onError: () => {},
    throwOnError: false,
    meta: {
      mapError: mapAxiosError
    }
  });
}
