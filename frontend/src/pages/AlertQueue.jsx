import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import Navbar from '../components/Navbar';
import PaginatedTable from '../components/PaginatedTable';
import RiskScoreBadge from '../components/RiskScoreBadge';
import StatusChip from '../components/StatusChip';
import ConfirmModal from '../components/ConfirmModal';

const STATUS_OPTIONS = ['OPEN', 'ACKNOWLEDGED', 'DISMISSED', 'ESCALATED'];

const COLUMNS = (onAction) => [
  { key: 'riskScore', label: 'Risk Score', render: (v) => <RiskScoreBadge score={v} /> },
  { key: 'alertRef', label: 'Alert Ref', render: (v, row) => <Link to={`/alerts/${row.id}`} className="text-accent hover:underline">{v}</Link> },
  { key: 'ruleName', label: 'Rule' },
  { key: 'customerMasked', label: 'Customer' },
  { key: 'accountRef', label: 'Account Ref' },
  { key: 'amountInr', label: 'Amount (INR)', render: (v) => v != null ? `₹${Number(v).toLocaleString('en-IN')}` : '—' },
  { key: 'createdAt', label: 'Created At', render: (v) => new Date(v).toLocaleString() },
  { key: 'status', label: 'Status', render: (v) => <StatusChip value={v} type="alert" /> },
  {
    key: 'id', label: 'Actions',
    render: (id, row) => (
      <div className="flex gap-1">
        {row.status === 'OPEN' && (
          <>
            <button onClick={() => onAction('acknowledge', row)} className="px-2 py-1 text-xs rounded bg-accent/20 text-accent hover:bg-accent/30">Ack</button>
            <button onClick={() => onAction('dismiss', row)} className="px-2 py-1 text-xs rounded bg-gray-500/20 text-gray-400 hover:bg-gray-500/30">Dismiss</button>
            <button onClick={() => onAction('escalate', row)} className="px-2 py-1 text-xs rounded bg-purple-500/20 text-purple-400 hover:bg-purple-500/30">Escalate</button>
          </>
        )}
      </div>
    ),
  },
];

export default function AlertQueue() {
  const [alerts, setAlerts] = useState([]);
  const [cursor, setCursor] = useState(null);
  const [hasMore, setHasMore] = useState(false);
  const [size, setSize] = useState(25);
  const [loadingMore, setLoadingMore] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [filters, setFilters] = useState({ statuses: ['OPEN'], ruleCode: '', dateFrom: '', dateTo: '', minScore: 0 });
  const [modal, setModal] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [dismissReason, setDismissReason] = useState('');
  const [escalateCaseRef, setEscalateCaseRef] = useState('');
  const [escalateMode, setEscalateMode] = useState('new');

  const buildParams = useCallback((nextCursor) => {
    const params = { size };
    if (filters.statuses.length) params.status = filters.statuses.join(',');
    if (filters.ruleCode) params.ruleCode = filters.ruleCode;
    if (filters.dateFrom) params.dateFrom = filters.dateFrom;
    if (filters.dateTo) params.dateTo = filters.dateTo;
    if (filters.minScore > 0) params.minScore = filters.minScore;
    if (nextCursor) params.cursor = nextCursor;
    return params;
  }, [filters, size]);

  // Reset and fetch first page whenever filters or size change
  const fetchFirst = useCallback(() => {
    const controller = new AbortController();
    setInitialLoading(true);
    api.get('/alerts', { params: buildParams(null), signal: controller.signal })
      .then(({ data }) => {
        setAlerts(data.content || data.items || data);
        setCursor(data.nextCursor ?? null);
        setHasMore(data.hasMore ?? false);
      })
      .catch(() => {})
      .finally(() => setInitialLoading(false));
    return controller;
  }, [buildParams]);

  useEffect(() => {
    const controller = fetchFirst();
    return () => controller.abort();
  }, [fetchFirst]);

  async function handleLoadMore() {
    if (!cursor) return;
    setLoadingMore(true);
    try {
      const { data } = await api.get('/alerts', { params: buildParams(cursor) });
      setAlerts((prev) => [...prev, ...(data.content || data.items || data)]);
      setCursor(data.nextCursor ?? null);
      setHasMore(data.hasMore ?? false);
    } finally {
      setLoadingMore(false);
    }
  }

  function handleSizeChange(s) {
    setSize(s);
  }

  function toggleStatus(s) {
    setFilters((f) => ({
      ...f,
      statuses: f.statuses.includes(s) ? f.statuses.filter((x) => x !== s) : [...f.statuses, s],
    }));
  }

  async function handleConfirm() {
    if (submitting) return;
    setSubmitting(true);
    const { type, row } = modal;
    try {
      if (type === 'acknowledge') await api.put(`/alerts/${row.id}/acknowledge`);
      if (type === 'dismiss') await api.put(`/alerts/${row.id}/dismiss`, { reason: dismissReason });
      if (type === 'escalate') {
        const caseId = escalateMode === 'new' ? null : escalateCaseRef || null;
        await api.put(`/alerts/${row.id}/escalate`, { caseId });
      }
      setModal(null);
      fetchFirst();
    } catch {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen bg-base">
      <Navbar />
      <main className="p-6">
        <h1 className="text-xl font-bold text-text-primary mb-4">Alert Queue</h1>

        <div className="bg-surface border border-subtle rounded-xl p-4 mb-5 flex flex-wrap gap-4 items-end">
          <div>
            <p className="text-xs text-text-muted mb-1 uppercase tracking-wide">Status</p>
            <div className="flex gap-2">
              {STATUS_OPTIONS.map((s) => (
                <button
                  key={s}
                  onClick={() => toggleStatus(s)}
                  className={`px-2 py-1 rounded text-xs font-medium transition-colors ${filters.statuses.includes(s) ? 'bg-accent text-white' : 'bg-card text-text-muted hover:bg-subtle'}`}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">Rule Code</label>
            <input value={filters.ruleCode} onChange={(e) => setFilters((f) => ({ ...f, ruleCode: e.target.value }))}
              className="bg-card border border-subtle rounded px-2 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent w-36" placeholder="All rules" />
          </div>
          <div>
            <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">From</label>
            <input type="date" value={filters.dateFrom} onChange={(e) => setFilters((f) => ({ ...f, dateFrom: e.target.value }))}
              className="bg-card border border-subtle rounded px-2 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent" />
          </div>
          <div>
            <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">To</label>
            <input type="date" value={filters.dateTo} onChange={(e) => setFilters((f) => ({ ...f, dateTo: e.target.value }))}
              className="bg-card border border-subtle rounded px-2 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent" />
          </div>
          <div className="flex-1 min-w-40">
            <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">Min Risk Score: {filters.minScore}</label>
            <input type="range" min="0" max="100" value={filters.minScore} onChange={(e) => setFilters((f) => ({ ...f, minScore: Number(e.target.value) }))}
              className="w-full accent-accent" />
          </div>
        </div>

        {initialLoading ? (
          <p className="text-text-muted text-sm">Loading…</p>
        ) : (
          <PaginatedTable
            columns={COLUMNS((type, row) => { setModal({ type, row }); setDismissReason(''); setEscalateCaseRef(''); setEscalateMode('new'); setSubmitting(false); })}
            data={alerts}
            hasMore={hasMore}
            onLoadMore={handleLoadMore}
            loading={loadingMore}
            size={size}
            onSizeChange={handleSizeChange}
          />
        )}
      </main>

      {modal?.type === 'acknowledge' && (
        <ConfirmModal title="Acknowledge Alert" onConfirm={handleConfirm} onCancel={() => { setModal(null); setSubmitting(false); }} confirmLabel="Acknowledge" submitting={submitting}>
          <p className="text-text-muted text-sm">Confirm acknowledgement of alert <strong className="text-text-primary">{modal.row.alertRef}</strong>?</p>
        </ConfirmModal>
      )}
      {modal?.type === 'dismiss' && (
        <ConfirmModal title="Dismiss Alert" onConfirm={handleConfirm} onCancel={() => { setModal(null); setSubmitting(false); }} confirmLabel="Dismiss" danger submitting={submitting}>
          <p className="text-text-muted text-sm mb-3">Reason for dismissal <span className="text-risk-high">*</span></p>
          <textarea value={dismissReason} onChange={(e) => setDismissReason(e.target.value)}
            className="w-full bg-card border border-subtle rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent resize-none h-20" placeholder="Enter reason…" />
        </ConfirmModal>
      )}
      {modal?.type === 'escalate' && (
        <ConfirmModal title="Escalate Alert" onConfirm={handleConfirm} onCancel={() => { setModal(null); setSubmitting(false); }} confirmLabel="Escalate" submitting={submitting}>
          <div className="flex gap-4 mb-4">
            <label className="flex items-center gap-2 text-sm text-text-primary cursor-pointer">
              <input type="radio" checked={escalateMode === 'new'} onChange={() => setEscalateMode('new')} className="accent-accent" /> Create new case
            </label>
            <label className="flex items-center gap-2 text-sm text-text-primary cursor-pointer">
              <input type="radio" checked={escalateMode === 'existing'} onChange={() => setEscalateMode('existing')} className="accent-accent" /> Link to existing case
            </label>
          </div>
          {escalateMode === 'existing' && (
            <input value={escalateCaseRef} onChange={(e) => setEscalateCaseRef(e.target.value)}
              className="w-full bg-card border border-subtle rounded px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent" placeholder="Case ref…" />
          )}
        </ConfirmModal>
      )}
    </div>
  );
}
