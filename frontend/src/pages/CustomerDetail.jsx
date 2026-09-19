import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../api/axios';
import Navbar from '../components/Navbar';
import RiskScoreBadge from '../components/RiskScoreBadge';
import StatusChip from '../components/StatusChip';

export default function CustomerDetail() {
  const { id } = useParams();
  const [customer, setCustomer] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetch() {
      setLoading(true);
      try {
        const { data } = await api.get(`/customers/${id}`);
        setCustomer(data);
      } finally {
        setLoading(false);
      }
    }
    fetch();
  }, [id]);

  if (loading) return <div className="min-h-screen bg-base"><Navbar /><p className="p-6 text-text-muted">Loading…</p></div>;
  if (!customer) return <div className="min-h-screen bg-base"><Navbar /><p className="p-6 text-text-muted">Customer not found.</p></div>;

  const fields = [
    ['Name', customer.name],
    ['Date of Birth', customer.dob],
    ['Email', customer.email],
    ['Phone', customer.phone],
    ['Address', customer.address],
    ['KYC Status', customer.kycStatus],
    ['Risk Rating', customer.riskRating],
    ['PEP Flag', customer.pepFlag ? 'Yes' : 'No'],
    ['Segment', customer.segment],
  ];

  return (
    <div className="min-h-screen bg-base">
      <Navbar />
      <main className="p-6 max-w-5xl mx-auto">
        <h1 className="text-xl font-bold text-text-primary mb-6">Customer Detail</h1>

        {/* PII fields grid */}
        <div className="bg-surface border border-subtle rounded-xl p-6 mb-6">
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {fields.map(([label, value]) => (
              <div key={label}>
                <p className="text-xs text-text-muted uppercase tracking-wide">{label}</p>
                <p className="text-text-primary text-sm mt-0.5">{value || '—'}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Accounts table */}
        <div className="bg-surface border border-subtle rounded-xl p-6 mb-6">
          <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wide mb-3">Accounts</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-text-muted text-xs uppercase tracking-wide">
                <tr>{['Ref', 'Type', 'Status', 'Balance', 'Tier'].map((h) => <th key={h} className="pb-2 pr-4 text-left">{h}</th>)}</tr>
              </thead>
              <tbody>
                {(customer.accounts || []).map((acc) => (
                  <tr key={acc.id} className="border-t border-subtle hover:bg-card/40">
                    <td className="py-2 pr-4"><Link to={`/accounts/${acc.id}`} className="text-accent hover:underline">{acc.ref}</Link></td>
                    <td className="py-2 pr-4 text-text-primary">{acc.type}</td>
                    <td className="py-2 pr-4"><StatusChip value={acc.status} /></td>
                    <td className="py-2 pr-4 text-text-primary">₹{Number(acc.balance).toLocaleString('en-IN')}</td>
                    <td className="py-2 pr-4 text-text-primary">{acc.tier}</td>
                  </tr>
                ))}
                {!customer.accounts?.length && <tr><td colSpan={5} className="py-4 text-text-muted">No accounts.</td></tr>}
              </tbody>
            </table>
          </div>
        </div>

        {/* Recent alerts */}
        <div className="bg-surface border border-subtle rounded-xl p-6">
          <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wide mb-3">Recent Alerts</h2>
          <div className="space-y-2">
            {(customer.recentAlerts || []).map((a) => (
              <Link key={a.id} to={`/alerts/${a.id}`} className="flex items-center gap-3 bg-card border border-subtle rounded-lg px-3 py-2 hover:border-accent/50 transition-colors">
                <RiskScoreBadge score={a.riskScore} />
                <span className="flex-1 text-text-primary text-sm">{a.ruleName}</span>
                <StatusChip value={a.status} type="alert" />
              </Link>
            ))}
            {!customer.recentAlerts?.length && <p className="text-text-muted text-sm">No recent alerts.</p>}
          </div>
        </div>
      </main>
    </div>
  );
}
