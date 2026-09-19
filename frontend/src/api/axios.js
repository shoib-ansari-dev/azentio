import axios from 'axios';
import { emitError } from '../components/GlobalErrorBanner';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL,
});

// Attach JWT from token store
api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 401 → clear token and redirect to login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      tokenStore.clear();
      window.location.href = '/login';
    } else {
      const msg = err.response?.data?.message || err.message || 'An error occurred';
      emitError(msg);
    }
    return Promise.reject(err);
  }
);

// Simple in-memory token holder shared between axios and AuthContext
export const tokenStore = (() => {
  let _token = null;
  return {
    get: () => _token,
    set: (t) => { _token = t; },
    clear: () => { _token = null; },
  };
})();

export default api;
