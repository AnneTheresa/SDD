import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { mapAxiosError } from '../api/axiosClient';
import { useDeposit } from '../hooks/useDeposit';
import { emptyMoneyMovementForm } from '../types';

function mapMoneyMovementError(error) {
  const mapped = mapAxiosError(error);
  const message = String(mapped.message || '').toLowerCase();

  if (message.includes('authenticated user not found') || message.includes('authentication required')) {
    return {
      ...mapped,
      message: 'Backend authentication failed for this money movement request. Please log out, log back in, and retry. If the issue persists, this matches the current known backend limitation for deposit/withdraw.'
    };
  }

  return mapped;
}

export function DepositPage() {
  const { accountId } = useParams();
  const deposit = useDeposit();
  const [form, setForm] = useState({ ...emptyMoneyMovementForm, accountId: accountId || '' });
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);

    try {
      const response = await deposit.mutateAsync({ ...form, accountId: accountId || form.accountId });
      setResult(response);
    } catch (requestError) {
      setResult(null);
      setError(mapMoneyMovementError(requestError));
    }
  }

  return (
    <div className="stack">
      <section className="panel stack">
        <div>
          <h2>Deposit Funds</h2>
          <p className="muted">Submit a deposit and inspect the updated account plus resulting transaction. A fresh idempotency key is generated automatically for each submit.</p>
        </div>
        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="deposit-account-id">Account ID</label>
            <input id="deposit-account-id" value={accountId || form.accountId} readOnly />
          </div>
          <div className="field">
            <label htmlFor="deposit-amount">Amount</label>
            <input
              id="deposit-amount"
              value={form.amount}
              onChange={(event) => setForm((current) => ({ ...current, amount: event.target.value }))}
            />
          </div>
          <div className="field full">
            <label htmlFor="deposit-description">Description</label>
            <input
              id="deposit-description"
              value={form.description}
              onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
            />
          </div>
          <div className="actions">
            <button type="submit" disabled={deposit.isPending}>Submit Deposit</button>
            <Link className="button-link subtle" to={`/accounts/${accountId}`}>Back to Account</Link>
          </div>
        </form>
        {error ? <div className="banner error">{error.message}</div> : null}
      </section>
      {result ? (
        <section className="panel">
          <div className="banner success">{result.message}</div>
        </section>
      ) : null}
    </div>
  );
}
