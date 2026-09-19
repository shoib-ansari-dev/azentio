import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../api/axios';
import Navbar from '../components/Navbar';
import StatusChip from '../components/StatusChip';
import AuditTimeline from '../components/AuditTimeline';
import ConfirmModal from '../components/ConfirmModal';

export default function AlertDetail() {
  const { id } = useParams();
  const [alert, setAlert] = useState(null);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null);
  const [dismissReason, setDismissReason] = useState('');
  const [escalateMode, setEscalateMode] = useState('new');
  const [caseRef, setCaseRef] = useState('');

  const fetchAlert = useCallback(() => {
    const controller = new AbortController();
    setLoading(true);
    api.get(`/alerts/${id}`, { signal: controller.signal })
      .then(({ data }) => setAlert(data))
      .catch(() => {})
      .finally(() => setLoading(false));
    return controller;
  }, [id]);

  useEffect(() => {
    const controller = fetchAlert();
    return () => controller.abort();
  }, [fetchAlert]);

  async function handleConfirm() {
    try {
      if (modal === 'acknowledge') await api.put(`/alerts/${id}/acknowledge`);
      if (modal === 'dismiss') await api.put(`/alerts/${id}/dismiss`, { reason: dismissReason });
      if (modal === 'escalate') {
        const caseId = escalateMode === 'new' ? null : caseRef || null;
        await api.put(`/alerts/${id}/escalate`, { caseId });
      }
      setModal(null);
      fetchAlert();
    } catch {}
  }

  if (loading) return <div className="min-h-screen bg-base"><Navbar /><p className="p-6 text-text-muted">Loading…</p></div>;
  if (!alert) return <div className="min-h-screen bg-base"><Navbar /><p className="p-6 text-text-muted">Alert not found.</p></div>;

  return (
    <div className="min-h-screen bg-base">
      <Navbar />
      <main className="p-6 max-w-6xl mx-auto">
        <div className="mb-4">
          <Link to="/alerts" className="text-accent text-sm hover:underline">← Alert Queue</Link>
        </div>

        {/* Two-column layout */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          {/* Left — Summary */}
          <div className="bg-surface border border-subtle rounded-xl p-6 space-y-4">
            <div className="flex items-center gap-4">
              <span className="text-5xl font-bold" style={{ color: alert.riskScore >= 70 ? '#ef4444' : alert.riskScore >= 40 ? '#f59e0b' : '#22c55e' }}>
                {alert.riskScore}
              </span>
              <StatusChip value={alert.status} type="alert" />
            </div>
            <div className="flex gap-2 flex-wrap">
              {alert.status === 'OPEN' && (
                <>
                  <button onClick={() => setModal('acknowledge')} className="px-3 py-1.5 text-xs rounded bg-accent/20 text-accent hover:bg-accent/30">Acknowledge</button>
                  <button onClick={() => setModal('dismiss')} className="px-3 py-1.5 text-xs rounded bg-gray-500/20 text-gray-400 hover:bg-gray-500/30">Dismiss</button>
                  <button onClick={() => setModal('escalate')} className="px-3 py-1.5 text-xs rounded bg-purple-500/20 text-purple-400 hover:bg-purple-500/30">Escalate</button>
                </>
              )}
            </div>
            <div className="space-y-2 text-sm">
              <div><span className="text-text-muted">Rule:</span> <span className="text-text-primary ml-2">{alert.ruleName}</span></div>
              <div><span className="text-text-muted">Explanation:</span> <p className="text-text-primary mt-1">{alert.explanation}</p></div>
              <div><span className="text-text-muted">Created:</span> <span className="text-text-primary ml-2">{new Date(alert.createdAt).toLocaleString()}</span></div>
              <div><span className="text-text-muted">Updated:</span> <span className="text-text-primary ml-2">{new Date(alert.updatedAt).toLocaleString()}</span></div>
            </div>
          </div>

          {/* Right — Evidence */}
          <div className="bg-surface border border-subtle rounded-xl p-6">
            <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wide mb-3">Evidence Transactions</h2>
            <div className="overflow-x-auto">
              <table className="w-full text-xs">
                <thead className="text-text-muted">
                  <tr>
                    {['Ref', 'Amount', 'Currency', 'Timestamp', 'Counterparty'].map((h) => (
                      <th key={h} className="pb-2 pr-3 text-left font-medium">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(alert.evidenceTransactions || []).map((tx) => (
                    <tr key={tx.ref} className="border-t border-subtle">
                      <td className="py-2 pr-3 text-text-primary">{tx.ref}</td>
                      <td className="py-2 pr-3 text-text-primary">{tx.amount}</td>
                      <td className="py-2 pr-3 text-text-primary">{tx.currency}</td>
                      <td className="py-2 pr-3 text-text-muted">{new Date(tx.timestamp).toLocaleString()}</td>
                      <td className="py-2 pr-3 text-text-muted">{tx.counterpartyJurisdiction}</td>
                    </tr>
                  ))}
                  {(!alert.evidenceTransactions?.length) && <tr><td colSpan={5} className="py-4 text-text-muted">No evidence transactions.</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Customer & Account strip */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          {alert.customer && (
            <div className="bg-surface border border-subtle rounded-xl p-4 space-y-1 text-sm">
              <p className="text-xs text-text-muted uppercase tracking-wide font-semibold mb-2">Customer</p>
              <p><span className="text-text-muted">Name:</span> <span className="text-text-primary ml-2">{alert.customer.maskedName}</span></p>
              <p><span className="text-text-muted">Segment:</span> <span className="text-text-primary ml-2">{alert.customer.segment}</span></p>
              <p><span className="text-text-muted">Risk Rating:</span> <span className="text-text-primary ml-2">{alert.customer.riskRating}</span></p>
              <p><span className="text-text-muted">KYC Status:</span> <span className="text-text-primary ml-2">{alert.customer.kycStatus}</span></p>
              <Link to={`/customers/${alert.customer.id}`} className="text-accent text-xs hover:underline mt-1 block">View Customer →</Link>
            </div>
          )}
          {alert.account && (
            <div className="bg-surface border border-subtle rounded-xl p-4 space-y-1 text-sm">
              <p className="text-xs text-text-muted uppercase tracking-wide font-semibold mb-2">Account</p>
              <p><span className="text-text-muted">Ref:</span> <span className="text-text-primary ml-2">{alert.account.ref}</span></p>
              <p><span className="text-text-muted">Type:</span> <span className="text-text-primary ml-2">{alert.account.type}</span></p>
              <p><span className="text-text-muted">Balance:</span> <span className="text-text-primary ml-2">₹{Number(alert.account.balance).toLocaleString('en-IN')}</span></p>
              <p><span className="text-text-muted">Tier:</span> <span className="text-text-primary ml-2">{alert.account.tier}</span></p>
              <Link to={`/accounts/${alert.account.id}`} className="text-accent text-xs hover:underline mt-1 block">View Account →</Link>
            </div>
          )}
        </div>

        <AuditTimeline entries={alert.auditTrail || []} />
      </main>

      {modal === 'acknowledge' && (
        <ConfirmModal title="Acknowledge Alert" onConfirm={handleConfirm} onCancel={() => setModal(null)} confirmLabel="Acknowledge">
          <p className="text-text-muted text-sm">Confirm acknowledgement of <strong className="text-text-primary">{alert.alertRef}</strong>?</p>
        </ConfirmModal>
      )}
      {modal === 'dismiss' && (
        <ConfirmModal title="Dismiss Alert" onConfirm={handleConfirm} onCancel={() => setModal(null)} confirmLabel="Dismiss" danger>
          <p className="text-text-muted text-sm mb-3">Reason for dismissal <span className="text-risk-high">*</span></p>
          <textarea value={dismissReason} onChange={(e) => setDismissReason(e.target.value)}
            className="w-full bg-card border border-subtle rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent resize-none h-20" placeholder="Enter reason…" />
        </ConfirmModal>
      )}
      {modal === 'escalate' && (
        <ConfirmModal title="Escalate Alert" onConfirm={handleConfirm} onCancel={() => setModal(null)} confirmLabel="Escalate">
          <div className="flex gap-4 mb-4">
            <label className="flex items-center gap-2 text-sm text-text-primary cursor-pointer"><input type="radio" checked={escalateMode === 'new'} onChange={() => setEscalateMode('new')} className="accent-accent" /> Create new case</label>
            <label className="flex items-center gap-2 text-sm text-text-primary cursor-pointer"><input type="radio" checked={escalateMode === 'existing'} onChange={() => setEscalateMode('existing')} className="accent-accent" /> Link existing</label>
          </div>
          {escalateMode === 'existing' && (
            <input value={caseRef} onChange={(e) => setCaseRef(e.target.value)} className="w-full bg-card border border-subtle rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent" placeholder="Case ref…" />
          )}
        </ConfirmModal>
      )}
    </div>
  );
}
