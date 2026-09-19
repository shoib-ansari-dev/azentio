import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import Navbar from '../components/Navbar';
import RiskScoreBadge from '../components/RiskScoreBadge';
import StatusChip from '../components/StatusChip';
import AuditTimeline from '../components/AuditTimeline';
import ConfirmModal from '../components/ConfirmModal';

export default function CaseDetail() {
  const { id } = useParams();
  const { role } = useAuth();
  const [caseData, setCaseData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notes, setNotes] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [closeModal, setCloseModal] = useState(false);
  const [disposition, setDisposition] = useState('');
  const [closeReason, setCloseReason] = useState('');

  const canEdit = role === 'SUPERVISOR' || role === 'ADMIN';

  const fetchCase = useCallback(() => {
    const controller = new AbortController();
    setLoading(true);
    api.get(`/cases/${id}`, { signal: controller.signal })
      .then(({ data }) => {
        setCaseData(data);
        setNotes(data.notes || '');
        setAssignedTo(data.assignedTo || '');
      })
      .catch(() => {})
      .finally(() => setLoading(false));
    return controller;
  }, [id]);

  useEffect(() => {
    const controller = fetchCase();
    return () => controller.abort();
  }, [fetchCase]);

  async function handleCloseCase() {
    try {
      await api.put(`/cases/${id}/close`, { disposition, reason: closeReason });
      setCloseModal(false);
      fetchCase();
    } catch {}
  }

  async function handleSaveNotes() {
    try {
      await api.put(`/cases/${id}`, { notes, assignedTo });
    } catch {}
  }

  if (loading) return <div className="min-h-screen bg-base"><Navbar /><p className="p-6 text-text-muted">Loading…</p></div>;
  if (!caseData) return <div className="min-h-screen bg-base"><Navbar /><p className="p-6 text-text-muted">Case not found.</p></div>;

  return (
    <div className="min-h-screen bg-base">
      <Navbar />
      <main className="p-6 max-w-6xl mx-auto">
        <div className="mb-4">
          <Link to="/cases" className="text-accent text-sm hover:underline">← Cases</Link>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          {/* Left — Case Info */}
          <div className="bg-surface border border-subtle rounded-xl p-6 space-y-4">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-lg font-semibold text-text-primary">{caseData.title}</h2>
                <p className="text-text-muted text-sm">{caseData.caseRef}</p>
              </div>
              <div className="flex gap-2">
                <StatusChip value={caseData.priority} type="priority" />
                <StatusChip value={caseData.status} type="case" />
              </div>
            </div>
            <div>
              <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">Assigned To</label>
              {canEdit ? (
                <input value={assignedTo} onChange={(e) => setAssignedTo(e.target.value)}
                  className="w-full bg-card border border-subtle rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent" />
              ) : (
                <p className="text-text-primary text-sm">{caseData.assignedTo || '—'}</p>
              )}
            </div>
            <div>
              <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">Notes</label>
              <textarea value={notes} onChange={(e) => setNotes(e.target.value)}
                disabled={!canEdit}
                className="w-full bg-card border border-subtle rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent resize-none h-28 disabled:opacity-60" />
            </div>
            {canEdit && (
              <div className="flex gap-2">
                <button onClick={handleSaveNotes} className="px-4 py-2 bg-accent hover:bg-blue-600 text-white text-sm rounded-lg transition-colors">Save</button>
                {caseData.status !== 'CLOSED' && (
                  <button onClick={() => setCloseModal(true)} className="px-4 py-2 bg-risk-high/20 hover:bg-risk-high/30 text-risk-high text-sm rounded-lg transition-colors">Close Case</button>
                )}
              </div>
            )}
          </div>

          {/* Right — Linked Alerts */}
          <div className="bg-surface border border-subtle rounded-xl p-6">
            <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wide mb-3">Linked Alerts ({caseData.linkedAlerts?.length || 0})</h2>
            <div className="space-y-3">
              {(caseData.linkedAlerts || []).map((a) => (
                <Link key={a.id} to={`/alerts/${a.id}`} className="block bg-card border border-subtle rounded-lg p-3 hover:border-accent/50 transition-colors">
                  <div className="flex items-center gap-3">
                    <RiskScoreBadge score={a.riskScore} />
                    <div className="flex-1 min-w-0">
                      <p className="text-text-primary text-sm font-medium truncate">{a.ruleName}</p>
                      <p className="text-text-muted text-xs">₹{Number(a.amountInr).toLocaleString('en-IN')}</p>
                    </div>
                    <StatusChip value={a.status} type="alert" />
                  </div>
                </Link>
              ))}
              {!caseData.linkedAlerts?.length && <p className="text-text-muted text-sm">No linked alerts.</p>}
            </div>
          </div>
        </div>

        <AuditTimeline entries={caseData.auditTrail || []} />
      </main>

      {closeModal && (
        <ConfirmModal title="Close Case" onConfirm={handleCloseCase} onCancel={() => setCloseModal(false)} confirmLabel="Close Case" danger>
          <div className="space-y-3">
            <div>
              <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">Disposition</label>
              <select value={disposition} onChange={(e) => setDisposition(e.target.value)}
                className="w-full bg-card border border-subtle rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent">
                <option value="">Select…</option>
                <option value="SAR_FILED">SAR Filed</option>
                <option value="NO_ACTION">No Action Required</option>
                <option value="REFERRED">Referred to Compliance</option>
              </select>
            </div>
            <div>
              <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">Reason</label>
              <textarea value={closeReason} onChange={(e) => setCloseReason(e.target.value)}
                className="w-full bg-card border border-subtle rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent resize-none h-20" placeholder="Enter reason…" />
            </div>
          </div>
        </ConfirmModal>
      )}
    </div>
  );
}
