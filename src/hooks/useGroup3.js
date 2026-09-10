import { useMutation, useQuery } from '@tanstack/react-query';
import {
  cancelStandingOrder,
  createStandingOrder,
  getMonthlyStatement,
  getSpendingInsights,
  getTransactionHistory,
  listStandingOrders,
  recategoriseTransaction
} from '../api/group3';

export function useTransactionHistory(filters) {
  return useQuery({
    queryKey: ['transaction-history', filters.accountId, filters.startDate, filters.endDate],
    queryFn: () => getTransactionHistory(filters),
    enabled: Boolean(filters.accountId && filters.startDate && filters.endDate)
  });
}

export function useStandingOrders(accountId) {
  return useQuery({
    queryKey: ['standing-orders', accountId],
    queryFn: () => listStandingOrders(accountId),
    enabled: Boolean(accountId)
  });
}

export function useCreateStandingOrder() {
  return useMutation({
    mutationFn: createStandingOrder,
    throwOnError: false
  });
}

export function useCancelStandingOrder() {
  return useMutation({
    mutationFn: cancelStandingOrder,
    throwOnError: false
  });
}

export function useMonthlyStatement(filters) {
  return useQuery({
    queryKey: ['monthly-statement', filters.accountId, filters.period],
    queryFn: () => getMonthlyStatement(filters),
    enabled: Boolean(filters.accountId && filters.period)
  });
}

export function useSpendingInsights(filters) {
  return useQuery({
    queryKey: ['spending-insights', filters.accountId, filters.period],
    queryFn: () => getSpendingInsights(filters),
    enabled: Boolean(filters.accountId && filters.period)
  });
}

export function useRecategoriseTransaction() {
  return useMutation({
    mutationFn: recategoriseTransaction,
    throwOnError: false
  });
}