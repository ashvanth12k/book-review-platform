import React from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import Dashboard from '../Dashboard';

export default function BooksPage() {
  const auth     = useAuth();
  const navigate = useNavigate();

  if (!auth || !auth.user) {
    return <Navigate to="/login" replace />;
  }

  const handleLogout = () => {
    auth.logout();
    navigate('/login', { replace: true });
  };

  return (
    <Dashboard
      onLogout={handleLogout}
      userRole={auth.user?.role}
      username={auth.user?.username || auth.user?.email}
      currentUser={auth.user}
    />
  );
}
