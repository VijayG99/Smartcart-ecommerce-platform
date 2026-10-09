import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import { errorMessage } from '../api';
import { ErrorBox } from '../components/Ui.jsx';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    if (!/^\S+@\S+\.\S+$/.test(form.email)) return setError('Enter a valid email address.');
    if (!form.password) return setError('Enter your password.');
    setError('');
    setLoading(true);
    try {
      const user = await login(form.email, form.password);
      navigate(location.state?.from || (user.role === 'ADMIN' ? '/admin/products' : '/'), { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="card narrow stack" onSubmit={submit} noValidate>
      <h1>Log in</h1>
      <ErrorBox message={error} />
      <label>Email<input type="email" value={form.email} onChange={set('email')} autoComplete="email" /></label>
      <label>Password<input type="password" value={form.password} onChange={set('password')} autoComplete="current-password" /></label>
      <button className="btn" disabled={loading}>{loading ? 'Logging in...' : 'Log in'}</button>
      <p className="muted">New here? <Link to="/register">Create an account</Link></p>
    </form>
  );
}
