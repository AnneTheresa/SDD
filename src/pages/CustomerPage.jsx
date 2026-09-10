import { useState } from 'react';
import { mapAxiosError } from '../api/axiosClient';
import { useDeleteCustomer } from '../hooks/useDeleteCustomer';

export function CustomerPage() {
  const [customerId, setCustomerId] = useState('100');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const deleteCustomer = useDeleteCustomer();

  async function handleDelete() {
    setError(null);
    setMessage(null);

    try {
      const result = await deleteCustomer.mutateAsync(customerId);
      setMessage(result.message);
    } catch (mutationError) {
      setError(mapAxiosError(mutationError));
    }
  }

  return (
    <div className="stack">
      <section className="panel stack">
        <div>
          <p className="eyebrow">DELETE /customers/{'{customerId}'}</p>
          <h2>Delete Customer</h2>
        </div>
        <p className="muted">
          This route is admin-only and succeeds only when the customer has no active accounts.
        </p>
        {message ? <div className="banner success">{message}</div> : null}
        {error ? <div className="banner error">{error.message}</div> : null}
        <div className="actions">
          <div className="field grow-field">
            <label htmlFor="customer-delete-id">Customer ID</label>
            <input
              id="customer-delete-id"
              value={customerId}
              onChange={(event) => setCustomerId(event.target.value)}
            />
          </div>
          <button type="button" onClick={handleDelete} disabled={deleteCustomer.isPending}>Delete Customer</button>
        </div>
      </section>
    </div>
  );
}
