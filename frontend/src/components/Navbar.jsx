import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const navItems = [
  { to: '/alerts', label: 'Alerts', roles: ['ANALYST', 'SUPERVISOR', 'ADMIN'] },
  { to: '/cases', label: 'Cases', roles: ['ANALYST', 'SUPERVISOR', 'ADMIN'] },
  { to: '/rules', label: 'Rules', roles: ['ADMIN'] },
  { to: '/jobs', label: 'Jobs', roles: ['ADMIN'] },
];

export default function Navbar() {
  const { role, user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <nav className="bg-surface border-b border-subtle px-6 py-3 flex items-center gap-6">
      <span className="text-accent font-bold text-lg tracking-tight mr-4">Sentinel AML</span>
      <div className="flex gap-1 flex-1">
        {navItems
          .filter((item) => item.roles.includes(role))
          .map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `px-3 py-1.5 rounded text-sm font-medium transition-colors ${
                  isActive ? 'bg-accent/20 text-accent' : 'text-text-muted hover:text-text-primary hover:bg-card'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
      </div>
      <div className="flex items-center gap-3 text-sm text-text-muted">
        <span>{user}</span>
        <span className="px-2 py-0.5 rounded bg-subtle text-xs uppercase">{role}</span>
        <button onClick={handleLogout} className="hover:text-risk-high transition-colors">Logout</button>
      </div>
    </nav>
  );
}
