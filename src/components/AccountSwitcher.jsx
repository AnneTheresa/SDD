import { useNavigate } from 'react-router-dom';

/**
 * Contextual account switcher shown at the top of every feature page.
 * Navigates to the same feature route for the newly selected account.
 *
 * @param {string|number} accountId  – currently active account ID
 * @param {Array}         accounts   – list from useListCustomerAccounts
 * @param {string}        feature    – URL segment: transactions | statements | insights | standing-orders
 */
export function AccountSwitcher({ accountId, accounts, feature }) {
  const navigate = useNavigate();

  const activeAccounts = (accounts || []).filter((a) => a.status === 'ACTIVE');

  if (activeAccounts.length === 0) {
    return null;
  }

  function handleChange(event) {
    const newId = event.target.value;
    if (newId && newId !== String(accountId)) {
      navigate(`/accounts/${newId}/${feature}`);
    }
  }

  return (
    <div className="account-switcher">
      <label className="account-switcher-label" htmlFor="account-switcher-select">
        Viewing Account
      </label>
      <select
        id="account-switcher-select"
        className="account-switcher-select"
        value={String(accountId)}
        onChange={handleChange}
      >
        {activeAccounts.map((account) => (
          <option key={account.accountId} value={String(account.accountId)}>
            #{account.accountId} · {account.accountType} · {account.balance}
          </option>
        ))}
      </select>
    </div>
  );
}
