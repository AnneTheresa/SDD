import { useState } from 'react';
import { Link } from 'react-router-dom';
import { isEmailLike } from '../types';

export function PasswordResetPage() {
  const [username, setUsername] = useState('');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  function handleSubmit(event) {
    event.preventDefault();
    setError(null);

    if (!isEmailLike(username)) {
      setError('Enter a valid email address.');
      return;
    }

    setMessage('Password reset is planned in the frontend flow, but the current backend does not expose this endpoint yet.');
  }

  return (
    <section className="panel stack auth-panel-page">
      <div>
        <p className="eyebrow">login-api</p>
        <h2>Password Reset Request</h2>
        <p className="muted">This route is present so the app structure matches the spec. The backend endpoint is not wired in the current service code.</p>
      </div>
      {message ? <div className="banner success">{message}</div> : null}
      {error ? <div className="banner error">{error}</div> : null}
      <form className="stack" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="password-reset-email">Email</label>
          <input
            id="password-reset-email"
            type="email"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
          />
        </div>
        <div className="actions">
          <button type="submit">Request Reset</button>
          <Link className="button-link subtle" to="/login">Back to Login</Link>
        </div>
      </form>
    </section>
  );
}