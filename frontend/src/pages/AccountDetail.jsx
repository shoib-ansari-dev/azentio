import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../api/axios';
import Navbar from '../components/Navbar';
import RiskScoreBadge from '../components/RiskScoreBadge';
import StatusChip from '../components/StatusChip';
import PaginatedTable from '../components/PaginatedTable';

const TX_COLUMNS = [
  { key: 'ref', label: 'Ref' },
  { key: 'amount', label: 'Amount (INR)', render: (v, row) => `${row.originalAmount} ${row.currency} / ₹${Number(v).toLocaleString('en-IN')}` },
  { key: 'type', label: 'Type' },
  { key: 'channel', label: 'Channel' },
  { key: 'counterpartyJurisdiction', label: 'Counterparty' },
  { key: 'timestamp', label: 'Timestamp', render: (v) => new Date(v).toLocaleString() },
];

export default function AccountDetail() {
  const { id } = useParams();
  const [account, setAccount] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetch() {
      setLoading(true);
      try {
        const [accRes, txRes] = await Promise.all([
          api.get(`/accounts/${id}`),
          api.get(`/accounts/${id}/transactions`),
        ]);
        setAccount(accRes.data);
        setTransactions(txRes.data.content || txRes.data);
      } finally {
        setLoading(false);
      }
    }
    fetch();
  }, [id]);

  if (loading) return <div className="min-h-screen bg-base"><Navbar /><p className="p-6 text-text-muted">Loading…</p></div>;
  if (!account) return <div className="min-h-screen bg-base"><Navbar /><p className="p-6 text-text-muted">Account not found.</p></div>;

  const fields = [
    ['Ref', account.ref], ['Type', account.type], ['Status', account.status],
    ['Balance', `₹${Number(account.balance).toLocaleString('en-IN')}`],
    ['Avg Balance', `₹${Number(account.avgBalance).toLocaleString('en-IN')}`],
    ['Branch', account.branch], ['Tier', account.tier], ['Risk Rating', account.riskRating],
  ];

  return (
    <div className="min-h-screen bg-base">
      <Navbar />
      <main className="p-6 max-w-5xl mx-auto">
        <h1 className="text-xl font-bold text-text-primary mb-6">Account Detail</h1>

        {/* Active alerts badge */}
        {account.activeAlertsCount > 0 && (
          <div className="mb-4 inline-flex items-center gap-2 bg-risk-high/15 border border-risk-high/30 rounded-lg px-3 py-2 text-sm">
            <span className="text-risk-high font-semibold">{account.activeAlertsCount} Active Alert{account.activeAlertsCount !== 1 ? 's' : ''}</span>
          </div>
        )}

        {/* Account fields */}
        <div className="bg-surface border border-subtle rounded-xl p-6 mb-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {fields.map(([label, value]) => (
              <div key={label}>
                <p className="text-xs text-text-muted uppercase tracking-wide">{label}</p>
                <p className="text-text-primary text-sm mt-0.5">{value || '—'}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Transactions */}
        <div className="bg-surface border border-subtle rounded-xl p-6 mb-6">
          <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wide mb-3">Transactions (last 30 days)</h2>
          <PaginatedTable columns={TX_COLUMNS} data={transactions} />
        </div>

        {/* Active alerts list */}
        {account.activeAlerts?.length > 0 && (
          <div className="bg-surface border border-subtle rounded-xl p-6">
            <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wide mb-3">Active Alerts</h2>
            <div className="space-y-2">
              {account.activeAlerts.map((a) => (
                <Link key={a.id} to={`/alerts/${a.id}`} className="flex items-center gap-3 bg-card border border-subtle rounded-lg px-3 py-2 hover:border-accent/50 transition-colors">
                  <RiskScoreBadge score={a.riskScore} />
                  <span className="flex-1 text-text-primary text-sm">{a.ruleName}</span>
                  <StatusChip value={a.status} type="alert" />
                </Link>
              ))}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
