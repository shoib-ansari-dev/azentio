import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import Navbar from '../components/Navbar';
import PaginatedTable from '../components/PaginatedTable';
import StatusChip from '../components/StatusChip';

const STATUS_OPTIONS = ['OPEN', 'IN_REVIEW', 'CLOSED', 'REPORTED'];
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const COLUMNS = [
  { key: 'caseRef', label: 'Case Ref', render: (v, row) => <Link to={`/cases/${row.id}`} className="text-accent hover:underline">{v}</Link> },
  { key: 'title', label: 'Title' },
  { key: 'priority', label: 'Priority', render: (v) => <StatusChip value={v} type="priority" /> },
  { key: 'status', label: 'Status', render: (v) => <StatusChip value={v} type="case" /> },
  { key: 'linkedAlertsCount', label: 'Linked Alerts', render: (v) => <span className="bg-accent/20 text-accent px-2 py-0.5 rounded-full text-xs font-bold">{v}</span> },
  { key: 'assignedTo', label: 'Assigned To' },
  { key: 'createdAt', label: 'Created At', render: (v) => new Date(v).toLocaleString() },
];

export default function CaseList() {
  const [cases, setCases] = useState([]);
  const [cursor, setCursor] = useState(null);
  const [hasMore, setHasMore] = useState(false);
  const [size, setSize] = useState(25);
  const [loadingMore, setLoadingMore] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [filters, setFilters] = useState({ status: '', assignedTo: '', priority: '' });

  const buildParams = useCallback((nextCursor) => {
    const params = { size };
    if (filters.status) params.status = filters.status;
    if (filters.assignedTo) params.assignedTo = filters.assignedTo;
    if (filters.priority) params.priority = filters.priority;
    if (nextCursor) params.cursor = nextCursor;
    return params;
  }, [filters, size]);

  const fetchFirst = useCallback(() => {
    const controller = new AbortController();
    setInitialLoading(true);
    api.get('/cases', { params: buildParams(null), signal: controller.signal })
      .then(({ data }) => {
        setCases(data.content || data.items || data);
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
      const { data } = await api.get('/cases', { params: buildParams(cursor) });
      setCases((prev) => [...prev, ...(data.content || data.items || data)]);
      setCursor(data.nextCursor ?? null);
      setHasMore(data.hasMore ?? false);
    } finally {
      setLoadingMore(false);
    }
  }

  return (
    <div className="min-h-screen bg-base">
      <Navbar />
      <main className="p-6">
        <h1 className="text-xl font-bold text-text-primary mb-4">Cases</h1>

        <div className="bg-surface border border-subtle rounded-xl p-4 mb-5 flex flex-wrap gap-4 items-end">
          <div>
            <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">Status</label>
            <select value={filters.status} onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value }))}
              className="bg-card border border-subtle rounded px-2 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent">
              <option value="">All</option>
              {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">Priority</label>
            <select value={filters.priority} onChange={(e) => setFilters((f) => ({ ...f, priority: e.target.value }))}
              className="bg-card border border-subtle rounded px-2 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent">
              <option value="">All</option>
              {PRIORITY_OPTIONS.map((p) => <option key={p} value={p}>{p}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs text-text-muted mb-1 uppercase tracking-wide">Assigned To</label>
            <input value={filters.assignedTo} onChange={(e) => setFilters((f) => ({ ...f, assignedTo: e.target.value }))}
              className="bg-card border border-subtle rounded px-2 py-1.5 text-sm text-text-primary focus:outline-none focus:border-accent w-40" placeholder="Analyst name…" />
          </div>
        </div>

        {initialLoading ? (
          <p className="text-text-muted text-sm">Loading…</p>
        ) : (
          <PaginatedTable
            columns={COLUMNS}
            data={cases}
            hasMore={hasMore}
            onLoadMore={handleLoadMore}
            loading={loadingMore}
            size={size}
            onSizeChange={setSize}
          />
        )}
      </main>
    </div>
  );
}
