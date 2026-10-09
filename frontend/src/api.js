import axios from 'axios';

// All calls go to /api (proxied by Vite in dev, by nginx in Docker).
const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// An expired or invalid token on a protected call sends the user back to the login page.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isAuthCall = err.config?.url?.startsWith('/auth/');
    if (err.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export function errorMessage(err) {
  const data = err.response?.data;
  if (data?.validationErrors) return Object.values(data.validationErrors).join(', ');
  if (data?.message) return data.message;
  if (err.code === 'ERR_NETWORK') return 'Cannot reach the server. Check that the backend is running.';
  return 'Something went wrong. Please try again.';
}

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
export const money = (v) => inr.format(Number(v ?? 0));
export const dateTime = (v) => (v ? new Date(v).toLocaleString() : '');

export default api;
