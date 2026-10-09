import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';

/** Not logged in -> login page. Logged in with the wrong role -> home page. */
export default function ProtectedRoute({ roles, children }) {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}
