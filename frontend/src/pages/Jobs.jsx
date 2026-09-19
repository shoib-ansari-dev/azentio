import { useState, useEffect, useRef } from 'react';
import api from '../api/axios';
import Navbar from '../components/Navbar';

const JOB_STATUS_COLORS = {
  RUNNING: 'bg-accent/20 text-accent',
  COMPLETED: 'bg-risk-low/20 text-risk-low',
  FAILED: 'bg-risk-high/20 text-risk-high',
  PENDING: 'bg-gray-500/20 text-gray-400',
};

const TERMINAL = ['COMPLETED', 'FAILED'];

export default function Jobs() {
  const [files, setFiles] = useState({ customers: null, accounts: null, transactions: null });
  const [jobs, setJobs] = useState([]);
  const [uploading, setUploading] = useState(false);
  const pollRef = useRef(null);

  function stopPolling() {
    if (pollRef.current) { clearInterval(pollRef.current); pollRef.current = null; }
  }

  function pollJobs(jobIds) {
    stopPolling();
    pollRef.current = setInterval(async () => {
      const results = await Promise.all(
        jobIds.map((id) => api.get(`/jobs/${id}`).then(({ data }) => data).catch(() => null))
      );
      const valid = results.filter(Boolean);
      setJobs((prev) => {
        const map = Object.fromEntries(prev.map((j) => [j.jobId, j]));
        valid.forEach((j) => { map[j.jobId] = j; });
        return Object.values(map);
      });
      const allDone = valid.length > 0 && valid.every((j) => TERMINAL.includes(j.status));
      if (allDone) stopPolling();
    }, 5000);
  }

  useEffect(() => () => stopPolling(), []);

  async function handleSubmit(e) {
    e.preventDefault();
    setUploading(true);
    try {
      const uploads = [];
      if (files.customers) {
        const fd = new FormData(); fd.append('file', files.customers);
        uploads.push(api.post('/customers/bulk', fd).then(({ data }) => ({ ...data, type: 'CUSTOMERS' })));
      }
      if (files.accounts) {
        const fd = new FormData(); fd.append('file', files.accounts);
        uploads.push(api.post('/accounts/bulk', fd).then(({ data }) => ({ ...data, type: 'ACCOUNTS' })));
      }
      if (files.transactions) {
        const fd = new FormData(); fd.append('file', files.transactions);
        uploads.push(api.post('/transactions/bulk', fd).then(({ data }) => ({ ...data, type: 'TRANSACTIONS' })));
      }
      const newJobs = await Promise.all(uploads);
      setJobs((prev) => [...newJobs, ...prev]);
      pollJobs(newJobs.map((j) => j.jobId));
      setFiles({ customers: null, accounts: null, transactions: null });
    } catch {
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="min-h-screen bg-base">
      <Navbar />
      <main className="p-6 max-w-5xl mx-auto">
        <h1 className="text-xl font-bold text-text-primary mb-5">Ingestion Jobs</h1>

        <div className="bg-surface border border-subtle rounded-xl p-6 mb-6">
          <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wide mb-4">Upload CSV Files</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            {[['customers', 'Customers CSV'], ['accounts', 'Accounts CSV'], ['transactions', 'Transactions CSV']].map(([key, label]) => (
              <div key={key} className="flex items-center gap-4">
                <label className="w-40 text-sm text-text-muted">{label}</label>
                <input
                  type="file"
                  accept=".csv"
                  onChange={(e) => setFiles((f) => ({ ...f, [key]: e.target.files[0] }))}
                  className="text-sm text-text-primary file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:bg-accent/20 file:text-accent file:text-xs hover:file:bg-accent/30 cursor-pointer"
                />
                {files[key] && <span className="text-xs text-risk-low">{files[key].name}</span>}
              </div>
            ))}
            <button
              type="submit"
              disabled={uploading || (!files.customers && !files.accounts && !files.transactions)}
              className="mt-2 px-5 py-2 bg-accent hover:bg-blue-600 text-white text-sm font-semibold rounded-lg transition-colors disabled:opacity-50"
            >
              {uploading ? 'Uploading…' : 'Submit'}
            </button>
          </form>
        </div>

        <div className="bg-surface border border-subtle rounded-xl p-6">
          <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wide mb-3">Jobs</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="text-text-muted text-xs uppercase tracking-wide">
                <tr>
                  {['Job ID', 'Type', 'Status', 'Total', 'Processed', 'Failed', 'Submitted By', 'Started', 'Completed'].map((h) => (
                    <th key={h} className="pb-2 pr-4 font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {jobs.map((job) => (
                  <tr key={job.jobId} className="border-t border-subtle hover:bg-card/40">
                    <td className="py-2 pr-4 text-text-primary font-mono text-xs">{job.jobId}</td>
                    <td className="py-2 pr-4 text-text-primary">{job.type}</td>
                    <td className="py-2 pr-4">
                      <span className={`px-2 py-0.5 rounded text-xs font-semibold ${JOB_STATUS_COLORS[job.status] || 'bg-subtle text-text-muted'}`}>{job.status}</span>
                    </td>
                    <td className="py-2 pr-4 text-text-primary">{job.total ?? '—'}</td>
                    <td className="py-2 pr-4 text-risk-low">{job.processed ?? '—'}</td>
                    <td className="py-2 pr-4 text-risk-high">{job.failed ?? '—'}</td>
                    <td className="py-2 pr-4 text-text-muted">{job.submittedBy || '—'}</td>
                    <td className="py-2 pr-4 text-text-muted">{job.startedAt ? new Date(job.startedAt).toLocaleString() : '—'}</td>
                    <td className="py-2 pr-4 text-text-muted">{job.completedAt ? new Date(job.completedAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
                {!jobs.length && <tr><td colSpan={9} className="py-8 text-center text-text-muted">No jobs yet. Upload a CSV to get started.</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
}
