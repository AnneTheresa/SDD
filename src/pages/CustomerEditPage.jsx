import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { getCustomer, updateCustomer } from '../api/customers';
import { mapAxiosError } from '../api/axiosClient';
import { CUSTOMER_TYPES, emptyCustomerForm } from '../types';

export function CustomerEditPage() {
  const { customerId } = useParams();
  const [formState, setFormState] = useState(emptyCustomerForm);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const query = useQuery({
    queryKey: ['customer-edit', customerId],
    queryFn: () => getCustomer(customerId),
    enabled: Boolean(customerId)
  });
  const mutation = useMutation({
    mutationFn: (payload) => updateCustomer(customerId, payload)
  });

  useEffect(() => {
    if (query.data) {
      setFormState({
        name: query.data.name || '',
        address: query.data.address || '',
        type: query.data.type || 'INDIVIDUAL'
      });
    }
  }, [query.data]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setMessage(null);

    try {
      await mutation.mutateAsync(formState);
      setMessage('Customer updated successfully.');
      await query.refetch();
    } catch (requestError) {
      setError(mapAxiosError(requestError));
    }
  }

  return (
    <section className="panel stack">
      <div>
        <p className="eyebrow">PATCH /api/customers/{'{customerId}'}</p>
        <h2>Edit Customer</h2>
      </div>
      {query.isLoading ? <div className="banner success">Loading customer...</div> : null}
      {message ? <div className="banner success">{message}</div> : null}
      {error ? <div className="banner error">{error.message}</div> : null}
      {query.error ? <div className="banner error">{mapAxiosError(query.error).message}</div> : null}
      <form className="stack" onSubmit={handleSubmit}>
        <div className="form-grid">
          <div className="field">
            <label htmlFor="edit-customer-name">Name</label>
            <input
              id="edit-customer-name"
              value={formState.name}
              onChange={(event) => setFormState((current) => ({ ...current, name: event.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="edit-customer-type">Type</label>
            <select
              id="edit-customer-type"
              value={formState.type}
              onChange={(event) => setFormState((current) => ({ ...current, type: event.target.value }))}
            >
              {CUSTOMER_TYPES.map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </div>
          <div className="field full">
            <label htmlFor="edit-customer-address">Address</label>
            <input
              id="edit-customer-address"
              value={formState.address}
              onChange={(event) => setFormState((current) => ({ ...current, address: event.target.value }))}
            />
          </div>
        </div>
        <div className="actions">
          <button type="submit" disabled={mutation.isPending}>Save Changes</button>
          <Link className="button-link subtle" to={`/customer/${customerId}`}>Back to Customer</Link>
        </div>
      </form>
    </section>
  );
}