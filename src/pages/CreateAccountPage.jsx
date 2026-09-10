import { Navigate, useParams } from 'react-router-dom';

export function CreateAccountPage() {
  const { customerId } = useParams();
  return <Navigate to={customerId ? `/customer/${customerId}/accounts` : '/'} replace />;
}
