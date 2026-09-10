import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { createCustomer } from '../api/customers';
import { useAuth } from '../auth/AuthContext';
import { mapAxiosError } from '../api/axiosClient';
import { CUSTOMER_TYPES, emptyCustomerForm } from '../types';

export function CustomerCreatePage() {
  const navigate = useNavigate();
  const { rememberCustomerId } = useAuth();
  const [formState, setFormState] = useState(emptyCustomerForm);
  const [error, setError] = useState(null);
  const mutation = useMutation({ mutationFn: createCustomer });

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);

    try {
      const response = await mutation.mutateAsync(formState);
      rememberCustomerId(response.customerId);
      navigate(`/customer/${response.customerId}`);
    } catch (requestError) {
      setError(mapAxiosError(requestError));
    }
  }

  return (
    <section className="panel stack">
      <div>
        <p className="eyebrow">POST /api/customers</p>
        <h2>Create Customer Profile</h2>
        <p className="muted">Create the authenticated user&apos;s customer profile in the merged backend.</p>
      </div>
      {error ? <div className="banner error">{error.message}</div> : null}
      <form className="stack" onSubmit={handleSubmit}>
        <div className="form-grid">
          <div className="field">
            <label htmlFor="customer-name">Name</label>
            <input
              id="customer-name"
              value={formState.name}
              onChange={(event) => setFormState((current) => ({ ...current, name: event.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="customer-type">Customer Type</label>
            <select
              id="customer-type"
              value={formState.type}
              onChange={(event) => setFormState((current) => ({ ...current, type: event.target.value }))}
            >
              {CUSTOMER_TYPES.map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </div>
          <div className="field full">
            <label htmlFor="customer-address">Address</label>
            <input
              id="customer-address"
              value={formState.address}
              onChange={(event) => setFormState((current) => ({ ...current, address: event.target.value }))}
            />
          </div>
          {formState.type === 'COMPANY' && (
            <div className="field full">
              <label htmlFor="customer-gbn">Government Business Number</label>
              <input
                id="customer-gbn"
                value={formState.governmentBusinessNumber}
                onChange={(event) => setFormState((current) => ({ ...current, governmentBusinessNumber: event.target.value }))}
                placeholder="9-digit number"
                maxLength="9"
                pattern="[0-9]{9}"
              />
              <p className="field-hint">9-digit number.</p>
            </div>
          )}
        </div>
        <div className="actions">
          <button type="submit" disabled={mutation.isPending}>Create Customer</button>
        </div>
      </form>
    </section>
  );
}