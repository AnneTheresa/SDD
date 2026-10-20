import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';

export function DeveloperRoute() {
  const { isDeveloper } = useAuth();

  if (!isDeveloper) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
