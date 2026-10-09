import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api';
import { ErrorBox, Loading } from '../components/Ui.jsx';

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/users/profile').then((r) => setProfile(r.data)).catch((e) => setError(errorMessage(e)));
  }, []);

  if (!profile) return error ? <ErrorBox message={error} /> : <Loading />;
  return (
    <div className="card narrow stack">
      <h1>Your profile</h1>
      <dl>
        <dt>Name</dt><dd>{profile.name}</dd>
        <dt>Email</dt><dd>{profile.email}</dd>
        <dt>Role</dt><dd>{profile.role === 'ADMIN' ? 'Administrator' : 'Customer'}</dd>
      </dl>
    </div>
  );
}
