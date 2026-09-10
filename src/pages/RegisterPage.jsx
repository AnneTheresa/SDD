import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { loginUser, registerUser } from '../api/auth';
import { createCustomer } from '../api/customers';
import { useAuth } from '../auth/AuthContext';
import { mapAxiosError } from '../api/axiosClient';
import { CUSTOMER_TYPES, emptyRegisterForm, isEmailLike } from '../types';

const REGISTER_CUSTOMER_TYPE_LABELS = {
  PERSON: 'Personal',
  COMPANY: 'Business'
};

// Set to false to test KYC error boundaries
const KYC_VERIFIED_DEFAULT = true;

export function RegisterPage() {
  const navigate = useNavigate();
  const { completeLogin, rememberCustomerId } = useAuth();
  const [formState, setFormState] = useState(emptyRegisterForm);
  const [error, setError] = useState(null);
  const mutation = useMutation({
    mutationFn: async (form) => {
      const authPayload = {
        username: form.username,
        password: form.password
      };
      const customerPayload = {
        name: form.name,
        address: form.address,
        type: form.type,
        kycVerified: KYC_VERIFIED_DEFAULT
      };
      if (form.type === 'PERSON') {
        customerPayload.dateOfBirth = form.dateOfBirth || '1998-10-26';
      } else {
        customerPayload.governmentBusinessNumber = form.governmentBusinessNumber;
      }

      await registerUser(authPayload);
      const authResponse = await loginUser(authPayload);
      const customerResponse = await createCustomer(customerPayload, authResponse.accessToken);

      return {
        authResponse,
        customerResponse
      };
    }
  });
  const [step, setStep] = useState('selectType');
  const isPerson = formState.type === 'PERSON';

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);

    if (!isEmailLike(formState.username)) {
      setError({ message: 'Enter a valid email address.' });
      return;
    }

    if (!formState.password) {
      setError({ message: 'Password is required.' });
      return;
    }

    if (!formState.name.trim()) {
      setError({ message: 'Name is required.' });
      return;
    }

    if (!formState.address.trim()) {
      setError({ message: 'Address is required.' });
      return;
    }

    if (isPerson) {
      if (!formState.dateOfBirth) {
        setError({ message: 'Date of birth is required.' });
        return;
      }
      const dob = new Date(formState.dateOfBirth);
      const today = new Date();
      let age = today.getFullYear() - dob.getFullYear();
      const m = today.getMonth() - dob.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
        age--;
      }
      if (age < 18) {
        setError({ message: 'You must be 18 years or above to open an account.' });
        return;
      }
    }

    try {
      const { authResponse, customerResponse } = await mutation.mutateAsync(formState);
      // Set auth state and customer context for auto-login
      completeLogin(authResponse, formState.username);
      rememberCustomerId(customerResponse.customerId);
      // Redirect to accounts page with success message
      navigate(`/customer/${customerResponse.customerId}/accounts`, {
        state: { successMessage: 'Account created successfully! Welcome to Voltio.' }
      });
    } catch (requestError) {
      // Enhanced error handling for backend validation errors
      const err = requestError;
      const res = err?.response?.data;
      if (res && res.code === 'VALIDATION_FAILED') {
        let msg = res.message;
        if (res.details && typeof res.details === 'object') {
          // Show the first validation error found
          const detailMsgs = Object.values(res.details).filter(Boolean);
          if (detailMsgs.length > 0) {
            msg = detailMsgs[0];
          }
        }
        setError({ message: msg });
      } else if (res && res.message) {
        setError({ message: res.message });
      } else {
        setError(mapAxiosError(requestError));
      }
    }
  }

  // Map step to numeric for text logic
  const activeStep = step === 'selectType' ? 1 : 2;

  return (
    <div className="register-page">
      <div className="register-card stack">
        <div>
          <h2>Register</h2>
        </div>
        <div className="register-stepper">
          {activeStep === 1 && (
            <span className={`register-step ${step === 'selectType' ? 'active' : 'completed'}`}>1. Account Type</span>
          )}
          {activeStep === 2 && (
            <span className={`register-step ${step === 'details' ? 'active' : ''}`}>2. Details</span>
          )}
        </div>
        {error ? <div className="banner error">{error.message}</div> : null}
        <form className="stack" onSubmit={handleSubmit}>
          {step === 'selectType' ? (
            <div className="form-grid">
              <div className="field full">
                <label htmlFor="register-type">Account Type</label>
                <select
                  id="register-type"
                  value={formState.type}
                  onChange={(event) => setFormState((current) => ({ ...current, type: event.target.value }))}
                >
                  {CUSTOMER_TYPES.map((type) => (
                    <option key={type} value={type}>{REGISTER_CUSTOMER_TYPE_LABELS[type] || type}</option>
                  ))}
                </select>
              </div>
            </div>
          ) : (
            <div className="stack">
              <div className="field">
                <label htmlFor="register-username">Email</label>
                <input
                  id="register-username"
                  type="email"
                  value={formState.username}
                  onChange={(event) => setFormState((current) => ({ ...current, username: event.target.value }))}
                />
              </div>
              <div className="field">
                <label htmlFor="register-password">Password</label>
                <input
                  id="register-password"
                  type="password"
                  value={formState.password}
                  onChange={(event) => setFormState((current) => ({ ...current, password: event.target.value }))}
                />
                <p className="field-hint">Minimum 8 characters with uppercase, digit, and special character.</p>
              </div>
              <div className="field">
                <label htmlFor="register-name">{isPerson ? 'Full Name' : 'Company Name'}</label>
                <input
                  id="register-name"
                  value={formState.name}
                  onChange={(event) => setFormState((current) => ({ ...current, name: event.target.value }))}
                />
              </div>
              <div className="field">
                <label htmlFor="register-address">Address</label>
                <input
                  id="register-address"
                  value={formState.address}
                  onChange={(event) => setFormState((current) => ({ ...current, address: event.target.value }))}
                />
              </div>
              {isPerson ? (
                <div className="field">
                  <label htmlFor="register-dob">Date of Birth</label>
                  <input
                    id="register-dob"
                    type="date"
                    value={formState.dateOfBirth}
                    onChange={(event) => setFormState((current) => ({ ...current, dateOfBirth: event.target.value }))}
                  />
                  <p className="field-hint">Required for TFSA accounts. Defaults to 1998-10-26 if left blank.</p>
                </div>
              ) : (
                <div className="field">
                  <label htmlFor="register-gbn">Government Business Number</label>
                  <input
                    id="register-gbn"
                    value={formState.governmentBusinessNumber}
                    onChange={(event) => setFormState((current) => ({ ...current, governmentBusinessNumber: event.target.value }))}
                    placeholder="9-digit number"
                    maxLength="9"
                    pattern="[0-9]{9}"
                  />
                  <p className="field-hint">9-digit number required for business accounts.</p>
                </div>
              )}
            </div>
          )}
          <div className="actions">
            {step === 'selectType' ? (
              <>
                <button type="button" onClick={() => setStep('details')}>Continue</button>
                <Link className="button-link subtle" to="/login">Back to Login</Link>
              </>
            ) : (
              <>
                <button type="button" className="secondary" onClick={() => setStep('selectType')}>Back</button>
                <button type="submit" disabled={mutation.isPending}>Create Account</button>
              </>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}