import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { getCustomer, updateCustomer } from '../api/customers';
import { useAuth } from '../auth/AuthContext';
import { mapAxiosError } from '../api/axiosClient';
import { CUSTOMER_TYPES } from '../types';

const CUSTOMER_TYPE_LABELS = {
  PERSON: 'Personal',
  COMPANY: 'Business'
};

function getInitials(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0][0].toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function CustomerProfilePage() {
  const { authState, isAdmin } = useAuth();
  const customerId = authState.customerId;

  const { data: customer, isLoading, error: queryError, refetch } = useQuery({
    queryKey: ['customer', customerId],
    queryFn: () => getCustomer(customerId),
    enabled: Boolean(customerId)
  });

  // Section 1: Identity & Type
  const [isEditingIdentity, setIsEditingIdentity] = useState(false);
  const [identityDraft, setIdentityDraft] = useState({ name: '', type: '' });
  const [identityError, setIdentityError] = useState(null);
  const [identitySuccess, setIdentitySuccess] = useState(false);

  // Section 3: Location
  const [isEditingAddress, setIsEditingAddress] = useState(false);
  const [addressDraft, setAddressDraft] = useState('');
  const [addressError, setAddressError] = useState(null);
  const [addressSuccess, setAddressSuccess] = useState(false);

  const identityMutation = useMutation({
    mutationFn: (payload) => updateCustomer(customerId, payload)
  });

  const addressMutation = useMutation({
    mutationFn: (payload) => updateCustomer(customerId, payload)
  });

  function handleEditIdentity() {
    setIdentityDraft({ name: customer?.name || '', type: customer?.type || 'PERSON' });
    setIdentityError(null);
    setIdentitySuccess(false);
    setIsEditingIdentity(true);
  }

  function handleCancelIdentity() {
    setIsEditingIdentity(false);
    setIdentityError(null);
  }

  async function handleSaveIdentity() {
    setIdentityError(null);

    if (identityDraft.name.trim().length < 2) {
      setIdentityError({ message: 'Name must be at least 2 characters.' });
      return;
    }

    const payload = { name: identityDraft.name.trim() };
    if (isAdmin) {
      payload.type = identityDraft.type;
    }

    try {
      await identityMutation.mutateAsync(payload);
      setIsEditingIdentity(false);
      setIdentitySuccess(true);
      refetch();
    } catch (err) {
      setIdentityError(mapAxiosError(err));
    }
  }

  function handleEditAddress() {
    setAddressDraft(customer?.address || '');
    setAddressError(null);
    setAddressSuccess(false);
    setIsEditingAddress(true);
  }

  function handleCancelAddress() {
    setIsEditingAddress(false);
    setAddressError(null);
  }

  async function handleSaveAddress() {
    setAddressError(null);
    try {
      await addressMutation.mutateAsync({ address: addressDraft });
      setIsEditingAddress(false);
      setAddressSuccess(true);
      refetch();
    } catch (err) {
      setAddressError(mapAxiosError(err));
    }
  }

  if (isLoading) {
    return (
      <div className="profile-page">
        <div className="banner success">Loading profile…</div>
      </div>
    );
  }

  if (queryError) {
    return (
      <div className="profile-page">
        <div className="banner error">{mapAxiosError(queryError).message}</div>
      </div>
    );
  }

  const name = customer?.name || '—';
  const initials = getInitials(customer?.name);

  return (
    <div className="profile-page">

      {/* Section 1: Identity & Type */}
      <div className="profile-section">
        <div className="profile-section-header">
          <span className="profile-section-title">Identity &amp; Type</span>
        </div>
        <div className="panel stack">
          <div className="profile-identity-card">
            <div className="profile-avatar-circle">{initials}</div>
            <div>
              <p className="eyebrow">{CUSTOMER_TYPE_LABELS[customer?.type] || customer?.type} Account</p>
              <h2 className="profile-name">{name}</h2>
            </div>
          </div>
          {identityError && <div className="banner error">{identityError.message}</div>}
          {identitySuccess && !isEditingIdentity && (
            <div className="banner success">Profile updated.</div>
          )}
          {isEditingIdentity && (
            <>
              <div className="field">
                <label htmlFor="profile-name">Name</label>
                <input
                  id="profile-name"
                  value={identityDraft.name}
                  onChange={(e) => setIdentityDraft((d) => ({ ...d, name: e.target.value }))}
                />
                <p className="field-hint">Minimum 2 characters.</p>
              </div>
              {isAdmin && (
                <div className="field">
                  <label htmlFor="profile-type">Account Type</label>
                  <select
                    id="profile-type"
                    value={identityDraft.type}
                    onChange={(e) => setIdentityDraft((d) => ({ ...d, type: e.target.value }))}
                  >
                    {CUSTOMER_TYPES.map((t) => (
                      <option key={t} value={t}>{CUSTOMER_TYPE_LABELS[t] || t}</option>
                    ))}
                  </select>
                </div>
              )}
              <div className="actions">
                <button type="button" onClick={handleSaveIdentity} disabled={identityMutation.isPending}>
                  Update
                </button>
                <button type="button" className="secondary" onClick={handleCancelIdentity}>
                  Cancel
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Section 2: Contact Details (read-only) */}
      <div className="profile-section">
        <div className="profile-section-header">
          <span className="profile-section-title">Contact Details</span>
        </div>
        <div className="panel stack">
          <div className="field">
            <label htmlFor="profile-name">Full Name</label>
            <input
              id="profile-name"
              type="text"
              value={customer?.name || ''}
              disabled
              readOnly
            />
          </div>
          <div className="field">
            <label htmlFor="profile-email">Email</label>
            <input
              id="profile-email"
              type="email"
              value={authState.username || ''}
              disabled
              readOnly
            />
          </div>
          {customer?.type === 'PERSON' ? (
            <div className="field">
              <label htmlFor="profile-dob">Date of Birth</label>
              <input
                id="profile-dob"
                type="date"
                value={customer?.dateOfBirth || ''}
                disabled
                readOnly
              />
            </div>
          ) : (
            <div className="field">
              <label htmlFor="profile-gbn">Government Business Number</label>
              <input
                id="profile-gbn"
                value={customer?.governmentBusinessNumber || ''}
                disabled
                readOnly
              />
            </div>
          )}
          <p className="field-hint">Contact support to update this information.</p>
        </div>
      </div>

      {/* Section 3: Location */}
      <div className="profile-section">
        <div className="profile-section-header">
          <span className="profile-section-title">Location</span>
          {!isEditingAddress && (
            <button type="button" className="secondary profile-edit-btn" onClick={handleEditAddress}>
              Edit
            </button>
          )}
        </div>
        <div className="panel stack">
          {addressError && <div className="banner error">{addressError.message}</div>}
          {addressSuccess && !isEditingAddress && (
            <div className="banner success">Address updated.</div>
          )}
          <div className="field">
            <label htmlFor="profile-address">Address</label>
            <input
              id="profile-address"
              value={isEditingAddress ? addressDraft : (customer?.address || '')}
              readOnly={!isEditingAddress}
              onChange={(e) => setAddressDraft(e.target.value)}
              style={!isEditingAddress ? { pointerEvents: 'none' } : undefined}
            />
          </div>
          {isEditingAddress && (
            <div className="actions">
              <button type="button" onClick={handleSaveAddress} disabled={addressMutation.isPending}>
                Update
              </button>
              <button type="button" className="secondary" onClick={handleCancelAddress}>
                Cancel
              </button>
            </div>
          )}
        </div>
      </div>

    </div>
  );
}


