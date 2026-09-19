import React, { createContext, useContext, useState } from 'react';
import api, { tokenStore } from '../api/axios';

const AuthContext = createContext(null);

function parseRole(token) {
  try {
    return JSON.parse(atob(token.split('.')[1])).role || null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState({ token: null, role: null, user: null });

  async function login(username, password) {
    const { data } = await api.post('/auth/login', { username, password });
    const token = data.token;
    tokenStore.set(token);
    const role = parseRole(token);
    setAuth({ token, role, user: username });
  }

  function logout() {
    tokenStore.clear();
    setAuth({ token: null, role: null, user: null });
  }

  return (
    <AuthContext.Provider value={{ ...auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
