import { useMutation } from '@tanstack/react-query';
import { askDeveloperChat } from '../api/developerChat';

export function useDeveloperChat() {
  return useMutation({
    mutationFn: askDeveloperChat,
    throwOnError: false,
  });
}
