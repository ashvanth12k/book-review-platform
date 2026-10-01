import React, { createContext, useContext, useState, useCallback } from 'react';
import { logoutUser } from './api';

export const AuthContext = createContext(null);

/**
 * AuthProvider — manages JWT-based auth state.
 *
 * localStorage key: 'authUser'
 * Stored shape: { accessToken, refreshToken, user: { id, username, email, role } }
 *
 * The axios interceptor in api.js reads 'authUser' directly from localStorage
 * so it works outside the React tree.
 */
export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    try {
      const raw = localStorage.getItem('authUser');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  });

  /**
   * Call after a successful backend login.
   * @param {Object} authData - { accessToken, refreshToken, user }
   */
  const login = useCallback((authData) => {
    localStorage.setItem('authUser', JSON.stringify(authData));
    setAuth(authData);
  }, []);

  /**
   * Revokes the refresh token on the backend, then clears local state.
   */
  const logout = useCallback(async () => {
    await logoutUser();           // POST /api/users/logout (errors are swallowed in api.js)
    localStorage.removeItem('authUser');
    setAuth(null);
  }, []);

  // Expose only the user object to consumers (keeps the same API as before)
  const user = auth?.user ?? null;

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
