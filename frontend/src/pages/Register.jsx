import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errorMessage } from '../api';
import { useAuth } from '../AuthContext.jsx';
import { ErrorBox } from '../components/Ui.jsx';

export default function Register() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  function validate() {
    if (form.name.trim().length < 2) return 'Enter your name.';
    if (!/^\S+@\S+\.\S+$/.test(form.email)) return 'Enter a valid email address.';
    if (form.password.length < 8) return 'Password must be at least 8 characters.';
    if (form.password !== form.confirm) return 'Passwords do not match.';
    return '';
  }

  async function submit(e) {
    e.preventDefault();
    const problem = validate();
    if (problem) return setError(problem);
    setError('');
    setLoading(true);
    try {
      await api.post('/auth/register', { name: form.name, email: form.email, password: form.password });
      await login(form.email, form.password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="card narrow stack" onSubmit={submit} noValidate>
      <h1>Create your account</h1>
      <ErrorBox message={error} />
      <label>Name<input value={form.name} onChange={set('name')} autoComplete="name" /></label>
      <label>Email<input type="email" value={form.email} onChange={set('email')} autoComplete="email" /></label>
      <label>Password<input type="password" value={form.password} onChange={set('password')} autoComplete="new-password" /></label>
      <label>Confirm password<input type="password" value={form.confirm} onChange={set('confirm')} autoComplete="new-password" /></label>
      <button className="btn" disabled={loading}>{loading ? 'Creating account...' : 'Create account'}</button>
      <p className="muted">Already registered? <Link to="/login">Log in</Link></p>
    </form>
  );
}
