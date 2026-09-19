import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function AdminRoute() {
  const { role } = useAuth();
  return role === 'ADMIN' ? <Outlet /> : <Navigate to="/alerts" replace />;
}
