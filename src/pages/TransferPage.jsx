import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { transferBetweenAccounts } from '../api/accounts';
import { mapAxiosError } from '../api/axiosClient';
import { useAuth } from '../auth/AuthContext';
import { useListCustomerAccounts } from '../hooks/useListCustomerAccounts';
import { useRecategoriseTransaction } from '../hooks/useGroup3';
import { TRANSACTION_CATEGORIES } from '../types';

function mapTransferError(error) {
  const mapped = mapAxiosError(error);
  const message = String(mapped.message || '').toLowerCase();

  if (message.includes('source account not found')) {
    return {
      ...mapped,
      message: 'Source account not found. Use an existing ACTIVE account ID that has not been deleted.'
    };
  }

  if (message.includes('destination account not found')) {
    return {
      ...mapped,
      message: 'Destination account not found. Use an existing ACTIVE account ID that has not been deleted.'
    };
  }

  return mapped;
}

function accountOptionLabel(account) {
  return `${account.accountType} - ${account.accountId} (Balance: $${account.balance})`;
}

export function TransferPage() {
  const [searchParams] = useSearchParams();
  const prefilledFromAccountId = searchParams.get('fromAccountId') || '';
  const { authState } = useAuth();
  const customerId = authState?.customerId;

  const accountsQuery = useListCustomerAccounts(customerId);
  const accounts = accountsQuery.data ?? [];

  const [form, setForm] = useState({
    fromAccountId: prefilledFromAccountId,
    toAccountId: '',
    amount: '25.00',
    description: '',
    category: ''
  });
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const transferMutation = useMutation({ mutationFn: transferBetweenAccounts });
  const recategoriseTransaction = useRecategoriseTransaction();

  const fromAccount = accounts.find((a) => String(a.accountId) === String(form.fromAccountId));
  const fromBalance = fromAccount ? Number(fromAccount.balance) : null;
  const sameAccountError =
    form.fromAccountId && form.toAccountId && form.fromAccountId === form.toAccountId
      ? 'Source and destination accounts must be different.'
      : null;
  const amountExceedsBalance =
    fromBalance !== null && Number(form.amount) > fromBalance
      ? `Amount cannot exceed the source account balance of $${fromBalance}.`
      : null;

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);

    if (sameAccountError) {
      setError({ message: sameAccountError });
      return;
    }

    if (amountExceedsBalance) {
      setError({ message: amountExceedsBalance });
      return;
    }

    const fromAccountId = Number.parseInt(String(form.fromAccountId).trim(), 10);
    const toAccountId = Number.parseInt(String(form.toAccountId).trim(), 10);
    const amount = Number.parseFloat(String(form.amount).trim());

    if (!fromAccountId || !toAccountId) {
      setError({ message: 'Please select both source and destination accounts.' });
      return;
    }

    if (!Number.isFinite(amount) || amount <= 0) {
      setError({ message: 'Amount must be greater than zero.' });
      return;
    }

    const payload = {
      ...form,
      fromAccountId,
      toAccountId,
      amount: String(form.amount).trim()
    };

    try {
      const response = await transferMutation.mutateAsync(payload);
      const category = String(form.category || '').trim();
      let nextResult = response;

      if (category && response?.debitTransaction?.transactionId) {
        try {
          await recategoriseTransaction.mutateAsync({
            accountId: payload.fromAccountId,
            transactionId: response.debitTransaction.transactionId,
            category
          });
          nextResult = {
            ...response,
            debitTransaction: {
              ...response.debitTransaction,
              category
            }
          };
        } catch (categoryError) {
          setError({
            message: `Transfer completed, but the selected category could not be saved. ${mapAxiosError(categoryError).message}`
          });
        }
      }

      setResult(nextResult);
    } catch (requestError) {
      setResult(null);
      setError(mapTransferError(requestError));
    }
  }

  const isLoading = accountsQuery.isLoading;

  return (
    <div className="stack">
      {error ? <div className="banner error">{error.message}</div> : null}
      {result ? <div className="banner success">{result.message || 'Transfer completed'}</div> : null}
      <section className="panel stack">
        <div>
          <h2>Transfer Funds</h2>
          <p className="muted">Move money between accounts and review both resulting transaction records.</p>
        </div>
        <form id="transfer-form" className="form-grid" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="transfer-from-account-id">From Account</label>
            <select
              id="transfer-from-account-id"
              value={form.fromAccountId}
              onChange={(event) => setForm((current) => ({ ...current, fromAccountId: event.target.value }))}
              disabled={isLoading}
              required
            >
              <option value="">{isLoading ? 'Loading accounts…' : 'Select source account'}</option>
              {accounts.map((account) => (
                <option key={account.accountId} value={String(account.accountId)}>
                  {accountOptionLabel(account)}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="transfer-to-account-id">To Account</label>
            <select
              id="transfer-to-account-id"
              value={form.toAccountId}
              onChange={(event) => setForm((current) => ({ ...current, toAccountId: event.target.value }))}
              disabled={isLoading}
              required
            >
              <option value="">{isLoading ? 'Loading accounts…' : 'Select destination account'}</option>
              {accounts.map((account) => (
                <option
                  key={account.accountId}
                  value={String(account.accountId)}
                  disabled={String(account.accountId) === String(form.fromAccountId)}
                >
                  {accountOptionLabel(account)}
                </option>
              ))}
            </select>
            {sameAccountError ? <p className="field-hint danger-text">{sameAccountError}</p> : null}
          </div>
          <div className="field">
            <label htmlFor="transfer-amount">Amount</label>
            <input
              id="transfer-amount"
              type="number"
              min="0.01"
              step="0.01"
              max={fromBalance !== null ? fromBalance : undefined}
              value={form.amount}
              onChange={(event) => setForm((current) => ({ ...current, amount: event.target.value }))}
              required
            />
            {amountExceedsBalance ? <p className="field-hint danger-text">{amountExceedsBalance}</p> : null}
            {fromBalance !== null ? <p className="field-hint">Available balance: ${fromBalance}</p> : null}
          </div>
          <div className="field full">
            <label htmlFor="transfer-description">Description</label>
            <input
              id="transfer-description"
              value={form.description}
              onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="transfer-category">Category</label>
            <select
              id="transfer-category"
              value={form.category}
              onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))}
            >
              <option value="">No category</option>
              {TRANSACTION_CATEGORIES.map((category) => <option key={category} value={category}>{category}</option>)}
            </select>
            <p className="field-hint">Optional. Category for this transaction.</p>
          </div>
        </form>
        <div className="actions centered-row">
          <button
            type="submit"
            form="transfer-form"
            disabled={transferMutation.isPending || isLoading || Boolean(sameAccountError) || Boolean(amountExceedsBalance)}
          >
            Submit Transfer
          </button>
          <Link className="button-link subtle" to={form.fromAccountId ? `/accounts/${form.fromAccountId}` : '/'}>Back</Link>
        </div>
      </section>
    </div>
  );
}
