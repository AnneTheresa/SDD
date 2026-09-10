import { useMutation } from '@tanstack/react-query';
import { askSavingsInsight, confirmAgentAction } from '../api/chat';

export function useSavingsChat() {
  return useMutation({
    mutationFn: askSavingsInsight,
    throwOnError: false
  });
}

export function useConfirmAgentAction() {
  return useMutation({
    mutationFn: confirmAgentAction,
    throwOnError: false
  });
}
