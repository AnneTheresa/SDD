import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { adminUpdateInterestRate, closeRrspAccount, deleteAccount, freezeAccount, getAccountControlHistory, getGics, openGic, redeemGic, unfreezeAccount } from '../api/accounts';
import { mapAxiosError } from '../api/axiosClient';
import { useAuth } from '../auth/AuthContext';
import { useGetAccount } from '../hooks/useGetAccount';

const GIC_TERM_LABELS = {
  SIX_MONTHS: '6 Month Term',
  ONE_YEAR: '1 Year Term',
  TWO_YEARS: '2 Year Term',
  THREE_YEARS: '3 Year Term',
  FIVE_YEARS: '5 Year Term'
};

const GIC_TERM_RATES = {
  SIX_MONTHS: 3,
  ONE_YEAR: 3.5,
  TWO_YEARS: 4,
  THREE_YEARS: 4.5,
  FIVE_YEARS: 5
};

function gicTermOptionLabel(term) {
  const label = GIC_TERM_LABELS[term] || term;
  const rate = GIC_TERM_RATES[term];
  return rate != null ? `${label} with ${rate.toFixed(2)}%` : label;
}

const GIC_TERMS = ['SIX_MONTHS', 'ONE_YEAR', 'TWO_YEARS', 'THREE_YEARS', 'FIVE_YEARS'];

function mapDeletedAccountError(error) {
  const mapped = mapAxiosError(error);
  if (mapped.code === 'ACCOUNT_NOT_FOUND' || mapped.message === 'Account not found') {
    return { ...mapped, message: 'This account may have been deleted or is no longer accessible.' };
  }
  return mapped;
}

export function AccountDetailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { accountId } = useParams();
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();
  const [error, setError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);
  const [interestRateInput, setInterestRateInput] = useState('');
  const [freezeReason, setFreezeReason] = useState('');
  const [releaseNote, setReleaseNote] = useState('');
  const [gicMessage, setGicMessage] = useState(null);
  const [gicError, setGicError] = useState(null);
  const [isGicModalOpen, setIsGicModalOpen] = useState(false);
  const [gicForm, setGicForm] = useState({ amount: '', term: 'SIX_MONTHS' });
  const query = useGetAccount(accountId);
  const deleteAccountMutation = useMutation({ mutationFn: deleteAccount });
  const closeRrspMutation = useMutation({ mutationFn: closeRrspAccount });
  const openGicMutation = useMutation({ mutationFn: ({ id, payload }) => openGic(id, payload) });
  const redeemGicMutation = useMutation({ mutationFn: ({ id, gicId }) => redeemGic(id, gicId) });
  const adminUpdateMutation = useMutation({ mutationFn: ({ id, rate }) => adminUpdateInterestRate(id, rate) });
  const freezeMutation = useMutation({ mutationFn: freezeAccount });
  const unfreezeMutation = useMutation({ mutationFn: unfreezeAccount });

  const account = query.data;
  const isRrsp = account?.accountType === 'RRSP';

  const gicQuery = useQuery({
    queryKey: ['gics', accountId],
    queryFn: () => getGics(accountId),
    enabled: isRrsp && Boolean(accountId)
  });

  const controlHistoryQuery = useQuery({
    queryKey: ['account-control-history', accountId],
    queryFn: () => getAccountControlHistory(accountId),
    enabled: isAdmin && Boolean(accountId)
  });

  async function handleDelete() {
    if (!query.data) return;
    setError(null);
    try {
      if (!window.confirm('Delete this account? This action is restricted to zero-balance accounts and cannot be undone from the UI.')) return;
      const result = await deleteAccountMutation.mutateAsync(query.data.accountId);
      navigate(query.data.customerId ? `/customer/${query.data.customerId}/accounts` : '/', {
        state: { deletedAccountMessage: result.message || `Account ${query.data.accountId} has been deleted.` }
      });
    } catch (mutationError) {
      setError(mapAxiosError(mutationError));
    }
  }

  async function handleCloseRrsp() {
    if (!account) return;
    setError(null);
    try {
      if (!window.confirm('Close this RRSP account? This action cannot be undone.')) return;
      await closeRrspMutation.mutateAsync(accountId);
      navigate(`/customer/${account.customerId}/accounts`, {
        state: { deletedAccountMessage: `RRSP Account ${accountId} has been closed.` }
      });
    } catch (mutationError) {
      setError(mapAxiosError(mutationError));
    }
  }

  async function handleOpenGic(event) {
    event.preventDefault();
    setGicError(null);
    setGicMessage(null);
    try {
      await openGicMutation.mutateAsync({ id: accountId, payload: gicForm });
      setGicMessage(`GIC opened successfully using funds from RRSP ${accountId}.`);
      setGicForm({ amount: '', term: 'SIX_MONTHS' });
      setIsGicModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['gics', accountId] });
      queryClient.invalidateQueries({ queryKey: ['account', accountId] });
    } catch (mutationError) {
      setGicError(mapAxiosError(mutationError));
    }
  }

  async function handleRedeemGic(gicId) {
    setGicError(null);
    setGicMessage(null);
    try {
      await redeemGicMutation.mutateAsync({ id: accountId, gicId });
      setGicMessage('GIC redeemed. Funds have been returned to your RRSP balance.');
      queryClient.invalidateQueries({ queryKey: ['gics', accountId] });
      queryClient.invalidateQueries({ queryKey: ['account', accountId] });
    } catch (mutationError) {
      setGicError(mapAxiosError(mutationError));
    }
  }

  async function handleAdminUpdateInterestRate(event) {
    event.preventDefault();
    setError(null);
    setActionMessage('Updating...');
    try {
      await adminUpdateMutation.mutateAsync({ id: accountId, rate: parseFloat(interestRateInput) });
      queryClient.invalidateQueries({ queryKey: ['account', accountId] });
      setActionMessage(`Interest rate updated to ${interestRateInput}%.`);
      setInterestRateInput('');
    } catch (mutationError) {
      setActionMessage(null);
      setError(mapAxiosError(mutationError));
    }
  }

  async function handleAdminDelete() {
    if (!account) return;
    setError(null);
    if (!window.confirm('Are you sure you want to permanently delete this account? This action cannot be undone.')) return;
    setActionMessage('Deleting...');
    try {
      await deleteAccountMutation.mutateAsync(account.accountId);
      navigate('/admin/accounts', {
        state: { deletedAccountMessage: `Account ${account.accountId} has been successfully removed.` }
      });
    } catch (mutationError) {
      setActionMessage(null);
      setError(mapAxiosError(mutationError));
    }
  }

  async function handleFreezeAccount(event) {
    event.preventDefault();
    if (!account || !freezeReason.trim()) return;
    setError(null);
    setActionMessage('Freezing account...');
    try {
      await freezeMutation.mutateAsync({
        accountId: account.accountId,
        reason: freezeReason.trim()
      });
      setFreezeReason('');
      setActionMessage('Account frozen successfully.');
      queryClient.invalidateQueries({ queryKey: ['account', accountId] });
      queryClient.invalidateQueries({ queryKey: ['account-control-history', accountId] });
    } catch (mutationError) {
      setActionMessage(null);
      setError(mapAxiosError(mutationError));
    }
  }

  async function handleUnfreezeAccount(event) {
    event.preventDefault();
    if (!account) return;
    setError(null);
    setActionMessage('Unfreezing account...');
    try {
      await unfreezeMutation.mutateAsync({
        accountId: account.accountId,
        notes: releaseNote.trim() || null
      });
      setReleaseNote('');
      setActionMessage('Account unfrozen successfully.');
      queryClient.invalidateQueries({ queryKey: ['account', accountId] });
      queryClient.invalidateQueries({ queryKey: ['account-control-history', accountId] });
    } catch (mutationError) {
      setActionMessage(null);
      setError(mapAxiosError(mutationError));
    }
  }

  const canDeleteAccount = Number(account?.balance) === 0;
  const queryError = query.error ? mapDeletedAccountError(query.error) : null;
  const gics = Array.isArray(gicQuery.data) ? gicQuery.data
    : Array.isArray(gicQuery.data?.gics) ? gicQuery.data.gics
    : [];

  return (
    <div className="stack">
      {query.isLoading ? <div className="banner success">Loading account...</div> : null}
      {error ? <div className="banner error">{error.message}</div> : null}
      {queryError ? <div className="banner error">{queryError.message}</div> : null}
      {account ? (
        <section className="panel stack">
          <div className="section-header">
            <div>
              <h2 className="title-large">Account Overview</h2>
              <p className="muted text-block-tight-top">View your account balance and access account features.</p>
            </div>
            <div className="actions">
              <Link className="button-link subtle" to={`/customer/${account.customerId}/accounts`}>Back to Account List</Link>
              {isRrsp ? (
                <button type="button" className="secondary danger" onClick={handleCloseRrsp} disabled={closeRrspMutation.isPending}>Close RRSP</button>
              ) : (
                <button type="button" className="secondary danger" onClick={handleDelete} disabled={deleteAccountMutation.isPending || !canDeleteAccount}>Delete Account</button>
              )}
            </div>
          </div>
          <div className="account-overview-grid">
            <div className="account-overview-col">
              <p className="muted account-overview-label">Account Type</p>
              <p className="account-overview-value">{account.accountType}</p>
            </div>
            <div className="account-overview-col split">
              <p className="muted account-overview-label">Balance</p>
              <p className="account-overview-value">{account.balance}</p>
            </div>
            <div className="account-overview-col split">
              <p className="muted account-overview-label">Status</p>
              <p className="account-overview-value">{account.status}</p>
            </div>
          </div>
          <div className="section-divider" />
          <div className="actions">
            <Link className="button-link subtle" to={`/accounts/transfer?fromAccountId=${account.accountId}`}>Transfer Funds</Link>
            <Link className="button-link subtle" to={`/accounts/${account.accountId}/transactions`}>Transaction History</Link>
            <Link className="button-link subtle" to={`/accounts/${account.accountId}/standing-orders`}>Standing Orders</Link>
            <Link className="button-link subtle" to={`/accounts/${account.accountId}/statements`}>Monthly Statement</Link>
            <Link className="button-link subtle" to={`/accounts/${account.accountId}/insights`}>Spending Insights</Link>
          </div>
          {!isRrsp && !canDeleteAccount ? <p className="muted compact-text">Balance must be exactly zero to delete this account.</p> : null}
          {location.pathname.endsWith('/edit') ? <div className="banner success">You are viewing the edit route for this account.</div> : null}
        </section>
      ) : null}

      {isRrsp ? (
        <section className="panel stack panel-alt">
          <div className="section-header">
            <div>
              <h3 className="zero-margin">GIC Portfolio</h3>
              <p className="muted text-block-tight-top">Guaranteed Investment Certificates linked to this RRSP.</p>
            </div>
            <button type="button" onClick={() => { setGicError(null); setGicMessage(null); setIsGicModalOpen(true); }}>
              Open GIC from this Account
            </button>
          </div>

          {gicMessage ? <div className="banner success">{gicMessage}</div> : null}
          {gicError ? <div className="banner error">{gicError.message}</div> : null}

          {gicQuery.isLoading ? <div className="banner success">Loading GICs...</div> : null}

          {gics.length > 0 ? (
            <section className="table-shell gic-table-shell">
              <table>
                <thead>
                  <tr>
                    <th>GIC ID</th>
                    <th className="col-number">Amount</th>
                    <th className="col-number">Interest Rate</th>
                    <th>Term</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {gics.map((gic) => (
                    <tr key={gic.gicId ?? gic.id}>
                      <td>{gic.gicId ?? gic.id}</td>
                      <td className="col-number">${gic.principalAmount ?? gic.amount}</td>
                      <td className="col-number">{GIC_TERM_RATES[gic.term] != null ? `${GIC_TERM_RATES[gic.term].toFixed(2)}%` : gic.interestRate != null ? `${(gic.interestRate * 100).toFixed(2)}%` : '—'}</td>
                      <td>{GIC_TERM_LABELS[gic.term] || gic.term}</td>
                      <td><span className="badge success-badge">{gic.status}</span></td>
                      <td>
                        <button
                          type="button"
                          className="secondary btn-redeem"
                          onClick={() => handleRedeemGic(gic.gicId ?? gic.id)}
                          disabled={redeemGicMutation.isPending}
                        >
                          Redeem
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          ) : !gicQuery.isLoading ? (
            <div className="panel gic-empty-panel">
              <p className="muted zero-margin">No active GIC investments for this RRSP.</p>
              <button type="button" onClick={() => { setGicError(null); setGicMessage(null); setIsGicModalOpen(true); }}>Open your first GIC</button>
            </div>
          ) : null}

        </section>
      ) : null}

      {isAdmin && account ? (
        <section className="panel stack danger-zone">
          <div className="section-header">
            <div>
              <h3 className="zero-margin">Admin Management</h3>
              <p className="muted text-block-tight-top">Administrative actions for this account.</p>
            </div>
          </div>
          {actionMessage ? <div className="banner success">{actionMessage}</div> : null}
          <div>
            <h4>Update Interest Rate</h4>
            <form className="stack" onSubmit={handleAdminUpdateInterestRate}>
              <div className="field">
                <label htmlFor="interest-rate-input">Interest Rate (%)</label>
                <input
                  id="interest-rate-input"
                  type="number"
                  step="0.01"
                  min="0"
                  value={interestRateInput}
                  onChange={(e) => setInterestRateInput(e.target.value)}
                  placeholder="e.g. 2.50"
                />
              </div>
              <div className="actions">
                <button type="submit" disabled={adminUpdateMutation.isPending || !interestRateInput}>Update Rate</button>
              </div>
            </form>
          </div>
          <div className="section-divider" />
          <div>
            <h4>Account Control</h4>
            {account.status === 'FROZEN' ? (
              <form className="stack" onSubmit={handleUnfreezeAccount}>
                <div className="field">
                  <label htmlFor="unfreeze-note">Release Note (optional)</label>
                  <input
                    id="unfreeze-note"
                    value={releaseNote}
                    onChange={(e) => setReleaseNote(e.target.value)}
                    placeholder="Case resolved"
                  />
                </div>
                <div className="actions">
                  <button type="submit" disabled={unfreezeMutation.isPending}>Unfreeze Account</button>
                </div>
              </form>
            ) : (
              <form className="stack" onSubmit={handleFreezeAccount}>
                <div className="field">
                  <label htmlFor="freeze-reason">Freeze Reason</label>
                  <input
                    id="freeze-reason"
                    value={freezeReason}
                    onChange={(e) => setFreezeReason(e.target.value)}
                    placeholder="Suspected account takeover"
                    required
                  />
                </div>
                <div className="actions">
                  <button type="submit" disabled={freezeMutation.isPending || !freezeReason.trim()}>Freeze Account</button>
                </div>
              </form>
            )}
          </div>
          <div className="section-divider" />
          <div>
            <h4>Delete Account</h4>
            <p className="muted compact-text">Permanently remove this account. This action cannot be undone.</p>
            <div className="actions" style={{ marginTop: "16px" }}>
              <button
                type="button"
                className="secondary danger"
                onClick={handleAdminDelete}
                disabled={deleteAccountMutation.isPending}
              >
                Delete Account
              </button>
            </div>
          </div>
          <div className="section-divider" />
          <div>
            <h4>Account Control History</h4>
            {controlHistoryQuery.isLoading ? <p className="muted compact-text">Loading control history...</p> : null}
            {!controlHistoryQuery.isLoading && (controlHistoryQuery.data?.events || []).length === 0 ? (
              <p className="muted compact-text">No freeze/unfreeze events recorded yet.</p>
            ) : null}
            {(controlHistoryQuery.data?.events || []).length > 0 ? (
              <ul className="stack">
                {controlHistoryQuery.data.events.map((event) => (
                  <li key={event.eventId} className="muted compact-text">
                    {event.actionType} {event.previousStatus} to {event.newStatus} at {event.timestamp}
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
        </section>
      ) : null}

      {isGicModalOpen ? (
        <div className="modal-backdrop" onClick={() => setIsGicModalOpen(false)}>
          <div className="modal-panel stack" role="dialog" aria-modal="true" aria-labelledby="gic-modal-title" onClick={(e) => e.stopPropagation()}>
            <div className="section-header">
              <h3 id="gic-modal-title">Open a GIC</h3>
              <button type="button" className="secondary" onClick={() => setIsGicModalOpen(false)} disabled={openGicMutation.isPending}>Close</button>
            </div>
            {gicError ? <div className="banner error">{gicError.message}</div> : null}
            <form className="stack" onSubmit={handleOpenGic}>
              <div className="stack">
                <div className="field">
                  <label htmlFor="gic-amount">Amount</label>
                  <input
                    id="gic-amount"
                    value={gicForm.amount}
                    onChange={(e) => setGicForm((f) => ({ ...f, amount: e.target.value }))}
                    placeholder="e.g. 1000.00"
                  />
                  <p className="field-hint">Will be deducted from your RRSP balance.</p>
                </div>
                <div className="field">
                  <label htmlFor="gic-term">Term</label>
                  <select
                    id="gic-term"
                    value={gicForm.term}
                    onChange={(e) => setGicForm((f) => ({ ...f, term: e.target.value }))}
                  >
                    {GIC_TERMS.map((t) => (
                      <option key={t} value={t}>{gicTermOptionLabel(t)}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="actions centered-actions">
                <button type="submit" disabled={openGicMutation.isPending}>Open GIC</button>
                <button type="button" className="secondary" onClick={() => setGicForm({ amount: '', term: 'SIX_MONTHS' })} disabled={openGicMutation.isPending}>Reset</button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  );
}

// export function AccountDetailPage() {
//   const navigate = useNavigate();
//   const location = useLocation();
//   const { accountId } = useParams();
//   const [error, setError] = useState(null);
//   const query = useGetAccount(accountId);
//   const deleteAccountMutation = useMutation({ mutationFn: deleteAccount });

//   async function handleDelete() {
//     if (!query.data) {
//       return;
//     }

//     setError(null);
//     try {
//       if (!window.confirm('Delete this account? This action is restricted to zero-balance accounts and cannot be undone from the UI.')) {
//         return;
//       }

//       const result = await deleteAccountMutation.mutateAsync(query.data.accountId);
//       navigate(query.data.customerId ? `/customer/${query.data.customerId}/accounts` : '/', {
//         state: {
//           deletedAccountMessage: result.message || `Account ${query.data.accountId} has been deleted.`
//         }
//       });
//     } catch (mutationError) {
//       setError(mapAxiosError(mutationError));
//     }
//   }

//   const account = query.data;
//   const canDeleteAccount = Number(account?.balance) === 0;
//   const queryError = query.error ? mapDeletedAccountError(query.error) : null;

//   return (
//     <div className="stack">
//       {query.isLoading ? <div className="banner success">Loading account...</div> : null}
//       {error ? <div className="banner error">{error.message}</div> : null}
//       {queryError ? <div className="banner error">{queryError.message}</div> : null}
//       {account ? (
//         <section className="panel stack">
//           <div className="section-header">
//             <div>
//               <h2 style={{ margin: 0, fontSize: '1.75rem' }}>Account Overview</h2>
//               <p className="muted" style={{ margin: '0.25rem 0 0 0' }}>View your account balance and access account features.</p>
//             </div>
//             <div className="actions">
//               <Link className="button-link subtle" to={`/customer/${account.customerId}/accounts`}>Back to Account List</Link>
//               <button type="button" className="secondary danger" onClick={handleDelete} disabled={deleteAccountMutation.isPending || !canDeleteAccount}>Delete Account</button>
//             </div>
//           </div>
//           <div style={{ display: 'flex', gap: '3rem', padding: '2.25rem 3rem', background: 'var(--color-surface, #f8f9fa)', borderRadius: '12px', border: '1px solid var(--color-border, #e2e6ea)', margin: '0.5rem 0 1.5rem 0', width: '100%', boxSizing: 'border-box', boxShadow: '0 2px 16px 0 rgba(0,0,0,0.04)' }}>
//             <div style={{ flex: 1, textAlign: 'center' }}>
//               <p className="muted" style={{ margin: '0 0 0.4rem 0', fontSize: '1rem', textAlign: 'center' }}>Account Type</p>
//               <p style={{ margin: 0, fontSize: '2.5rem', fontWeight: 700, lineHeight: 1, textAlign: 'center' }}>{account.accountType}</p>
//             </div>
//             <div style={{ flex: 1, borderLeft: '1px solid var(--color-border, #e2e6ea)', paddingLeft: '3rem', textAlign: 'center' }}>
//               <p className="muted" style={{ margin: '0 0 0.4rem 0', fontSize: '1rem', textAlign: 'center' }}>Balance</p>
//               <p style={{ margin: 0, fontSize: '2.5rem', fontWeight: 700, lineHeight: 1, textAlign: 'center' }}>{account.balance}</p>
//             </div>
//           </div>
//           <div className="section-divider" />
//           <div className="actions">
//             <Link className="button-link subtle" to={`/accounts/transfer?fromAccountId=${account.accountId}`}>Transfer Funds</Link>
//             <Link className="button-link subtle" to={`/accounts/${account.accountId}/transactions`}>Transaction History</Link>
//             <Link className="button-link subtle" to={`/accounts/${account.accountId}/standing-orders`}>Standing Orders</Link>
//             <Link className="button-link subtle" to={`/accounts/${account.accountId}/statements`}>Monthly Statement</Link>
//             <Link className="button-link subtle" to={`/accounts/${account.accountId}/insights`}>Spending Insights</Link>
//           </div>
//           {!canDeleteAccount ? <p className="muted compact-text">Balance must be exactly zero to delete this account.</p> : null}
//           {location.pathname.endsWith('/edit') ? <div className="banner success">You are viewing the edit route for this account.</div> : null}
//         </section>
//       ) : null}
//     </div>
//   );
// }

// function mapDeletedAccountError(error) {
//   const mapped = mapAxiosError(error);

//   if (mapped.code === 'ACCOUNT_NOT_FOUND' || mapped.message === 'Account not found') {
//     return {
//       ...mapped,
//       message: 'This account may have been deleted or is no longer accessible.'
//     };
//   }

//   return mapped;
// }
