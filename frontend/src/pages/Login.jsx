import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login, token } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (token) navigate('/alerts', { replace: true });
  }, [token, navigate]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(username, password);
      navigate('/alerts', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-base flex items-center justify-center">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-accent tracking-tight">Sentinel AML</h1>
          <p className="text-text-muted text-sm mt-1">Anti-Money Laundering Platform</p>
        </div>
        <form onSubmit={handleSubmit} className="bg-surface border border-subtle rounded-xl p-8 space-y-5">
          <div>
            <label className="block text-xs font-medium text-text-muted mb-1 uppercase tracking-wide">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full bg-card border border-subtle rounded-lg px-3 py-2.5 text-text-primary text-sm focus:outline-none focus:border-accent"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-text-muted mb-1 uppercase tracking-wide">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-card border border-subtle rounded-lg px-3 py-2.5 text-text-primary text-sm focus:outline-none focus:border-accent"
            />
          </div>
          {error && <p className="text-risk-high text-sm">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-accent hover:bg-blue-600 text-white font-semibold py-2.5 rounded-lg text-sm transition-colors disabled:opacity-50"
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  );
}
