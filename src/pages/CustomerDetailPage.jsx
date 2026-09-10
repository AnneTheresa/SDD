import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { deleteCustomer } from '../api/accounts';
import { getCustomer, listCustomers } from '../api/customers';
import { mapAxiosError } from '../api/axiosClient';
import { useAuth } from '../auth/AuthContext';

export function CustomerDetailPage() {
  const navigate = useNavigate();
  const { customerId } = useParams();
  const { authState, clearCustomerId, isAdmin, rememberCustomerId } = useAuth();
  const [error, setError] = useState(null);
  const query = useQuery({
    queryKey: ['customer', customerId],
    queryFn: () => getCustomer(customerId),
    enabled: Boolean(customerId)
  });
  const customerListQuery = useQuery({
    queryKey: ['customers'],
    queryFn: listCustomers,
    enabled: isAdmin
  });
  const deleteMutation = useMutation({ mutationFn: deleteCustomer });

  if (!isAdmin && !customerId && authState.customerId) {
    return <Navigate to={`/customer/${authState.customerId}`} replace />;
  }

  useEffect(() => {
    if (
      query.data &&
      customerId &&
      authState.customerId !== String(customerId) &&
      (isAdmin || !authState.customerId || authState.customerId === String(customerId))
    ) {
      rememberCustomerId(customerId);
    }
  }, [authState.customerId, customerId, isAdmin, query.data, rememberCustomerId]);

  function handleCustomerSwitch(event) {
    const nextCustomerId = event.target.value;
    if (!nextCustomerId) {
      return;
    }

    setError(null);
    rememberCustomerId(nextCustomerId);
    navigate(`/customer/${nextCustomerId}`);
  }

  async function handleDelete() {
    if (!customerId) {
      return;
    }

    setError(null);

    if (!window.confirm('Delete this customer? This succeeds only when there are no active accounts.')) {
      return;
    }

    try {
      await deleteMutation.mutateAsync(customerId);
      if (authState.customerId === String(customerId)) {
        clearCustomerId();
      }
      navigate('/');
    } catch (requestError) {
      setError(mapAxiosError(requestError));
    }
  }

  const customer = query.data;
  const showAdminSelectionOnly = isAdmin && !customerId;

  return (
    <div className="stack">
      <section className="panel stack">
        <div className="section-header">
          <div>
            <p className="eyebrow">{customerId ? 'GET /api/customers/{customerId}' : 'GET /api/customers'}</p>
            <h2>Customer Profile</h2>
            <p className="muted">{showAdminSelectionOnly ? 'Select a customer to open that profile.' : 'View the stored customer profile and branch into the account flows.'}</p>
          </div>
        </div>
        {isAdmin ? (
          <div className="field">
            <label htmlFor="customer-switcher">Admin Customer Switcher</label>
            <select id="customer-switcher" value={customerId || ''} onChange={handleCustomerSwitch}>
              <option value="">Select customer</option>
              {(customerListQuery.data || []).map((customerOption) => (
                <option key={customerOption.customerId} value={customerOption.customerId}>
                  {customerOption.customerId} - {customerOption.name}
                </option>
              ))}
            </select>
            {customerListQuery.error ? <p className="field-hint">{mapAxiosError(customerListQuery.error).message}</p> : null}
          </div>
        ) : null}
        {query.isLoading ? <div className="banner success">Loading customer profile...</div> : null}
        {error ? <div className="banner error">{error.message}</div> : null}
        {query.error ? <div className="banner error">{mapAxiosError(query.error).message}</div> : null}
        {showAdminSelectionOnly ? <div className="banner success">Choose a customer from the switcher to load their profile.</div> : null}
      </section>

      {customer ? (
        <section className="panel stack">
          <div className="detail-grid">
            <article className="detail-item">
              <p className="muted">Customer ID</p>
              <strong>{customer.customerId}</strong>
            </article>
            <article className="detail-item">
              <p className="muted">Name</p>
              <strong>{customer.name}</strong>
            </article>
            <article className="detail-item">
              <p className="muted">Type</p>
              <strong>{customer.type}</strong>
            </article>
            <article className="detail-item">
              <p className="muted">Address</p>
              <strong>{customer.address}</strong>
            </article>
          </div>
          <div className="actions">
            <Link className="button-link subtle" to={`/customer/${customerId}/edit`}>Edit Customer</Link>
            <Link className="button-link" to={`/customer/${customerId}/accounts`}>Accounts</Link>
            {isAdmin ?  <Link className="button-link" to={`/admin/${customerId}/risk-assessment`}>Risk Assessment</Link> : null}
            {isAdmin ? <button type="button" className="danger" onClick={handleDelete} disabled={deleteMutation.isPending}>Delete Customer</button> : null}
          </div>
        </section>
      ) : null}
    </div>
  );
}