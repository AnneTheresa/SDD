import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useListCustomerAccounts } from '../hooks/useListCustomerAccounts';

/**
 * Route wrapper that protects feature pages (transactions, statements, insights,
 * standing-orders).  If the authenticated customer has no ACTIVE accounts, they
 * are redirected to their accounts list page with a contextual flash message.
 * Admins are never redirected — they browse accounts by ID.
 */
export function FeatureGuard() {
  const { authState, isAdmin } = useAuth();
  const customerId = authState.customerId;
  const accountsQuery = useListCustomerAccounts(customerId);
  const location = useLocation();

  // Admins browse arbitrary accounts — skip the guard
  if (isAdmin) {
    return <Outlet />;
  }

  // While loading or data not yet available, render normally
  if (accountsQuery.isLoading || !accountsQuery.data) {
    return <Outlet />;
  }

  const activeAccounts = accountsQuery.data.filter((a) => a.status === 'ACTIVE');

  if (activeAccounts.length === 0 && customerId) {
    // Derive a human-readable feature name from the URL (e.g. "standing-orders" → "standing orders")
    const featureMatch = location.pathname.match(/\/accounts\/\d+\/([^/]+)$/);
    const featureName = featureMatch
      ? featureMatch[1].replace(/-/g, ' ')
      : 'this feature';

    return (
      <Navigate
        to={`/customer/${customerId}/accounts`}
        state={{ flash: `Please create an account to view ${featureName}.` }}
        replace
      />
    );
  }

  return <Outlet />;
}
