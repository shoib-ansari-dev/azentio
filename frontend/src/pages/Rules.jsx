import { useState, useEffect } from 'react';
import api from '../api/axios';
import Navbar from '../components/Navbar';

export default function Rules() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editingParams, setEditingParams] = useState({});
  const [saving, setSaving] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    api.get('/rules', { signal: controller.signal })
      .then(({ data }) => setRules(data))
      .catch(() => {})
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, []);

  async function toggleEnabled(rule) {
    setSaving(rule.id);
    try {
      const { data } = await api.put(`/rules/${rule.id}`, {
        enabled: !rule.enabled,
        parameters: rule.parameters ?? '{}',
      });
      setRules((prev) => prev.map((r) => r.id === rule.id ? data : r));
    } finally {
      setSaving(null);
    }
  }

  function openEdit(rule) {
    try {
      setEditingParams(JSON.parse(rule.parameters || '{}'));
    } catch {
      setEditingParams({});
    }
    setEditingId(rule.id);
  }

  async function saveParams(rule) {
    setSaving(rule.id);
    try {
      const { data } = await api.put(`/rules/${rule.id}`, {
        enabled: rule.enabled,
        parameters: JSON.stringify(editingParams),
      });
      setRules((prev) => prev.map((r) => r.id === rule.id ? data : r));
      setEditingId(null);
    } finally {
      setSaving(null);
    }
  }

  function formatLabel(key) {
    return key.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  return (
    <div className="min-h-screen bg-base">
      <Navbar />
      <main className="p-6">
        <h1 className="text-xl font-bold text-text-primary mb-5">Rule Configuration</h1>
        {loading ? (
          <p className="text-text-muted text-sm">Loading…</p>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-subtle">
            <table className="w-full text-sm text-left">
              <thead className="bg-card text-text-muted uppercase text-xs tracking-wide">
                <tr>
                  {['Rule Code', 'Name', 'Enabled', 'Parameters', 'Last Updated By', 'At'].map((h) => (
                    <th key={h} className="px-4 py-3">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rules.map((rule) => (
                  <tr key={rule.id} className="border-t border-subtle hover:bg-card/50 transition-colors">
                    <td className="px-4 py-3 text-text-primary font-mono text-xs">{rule.ruleCode}</td>
                    <td className="px-4 py-3 text-text-primary">{rule.name}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => toggleEnabled(rule)}
                        disabled={saving === rule.id}
                        className={`relative inline-flex h-5 w-9 rounded-full transition-colors ${rule.enabled ? 'bg-accent' : 'bg-subtle'} disabled:opacity-50`}
                      >
                        <span className={`inline-block h-4 w-4 rounded-full bg-white shadow transition-transform mt-0.5 ${rule.enabled ? 'translate-x-4' : 'translate-x-0.5'}`} />
                      </button>
                    </td>
                    <td className="px-4 py-3 min-w-48">
                      {editingId === rule.id ? (
                        <div className="flex flex-col gap-2">
                          {Object.entries(editingParams).map(([key, val]) => (
                            <div key={key} className="flex items-center gap-2">
                              <label className="text-xs text-text-muted w-32 shrink-0">{formatLabel(key)}</label>
                              <input
                                type="number"
                                value={val}
                                onChange={(e) => setEditingParams((p) => ({ ...p, [key]: Number(e.target.value) }))}
                                className="bg-card border border-accent rounded px-2 py-1 text-sm text-text-primary focus:outline-none w-32"
                              />
                            </div>
                          ))}
                          <div className="flex gap-1 mt-1">
                            <button onClick={() => saveParams(rule)} disabled={saving === rule.id} className="px-2 py-0.5 text-xs bg-accent text-white rounded disabled:opacity-50">Save</button>
                            <button onClick={() => setEditingId(null)} className="px-2 py-0.5 text-xs bg-subtle text-text-muted rounded">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <div className="flex items-center gap-3">
                          <span className="text-xs text-text-muted font-mono">
                            {Object.entries(JSON.parse(rule.parameters || '{}')).map(([k, v]) => `${formatLabel(k)}: ${v}`).join(', ') || '—'}
                          </span>
                          <button
                            onClick={() => openEdit(rule)}
                            className="px-2 py-1 text-xs bg-card border border-subtle rounded text-text-muted hover:text-text-primary hover:border-accent transition-colors shrink-0"
                          >
                            Edit
                          </button>
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 text-text-muted">{rule.updatedBy || '—'}</td>
                    <td className="px-4 py-3 text-text-muted">{rule.updatedAt ? new Date(rule.updatedAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
                {!rules.length && <tr><td colSpan={6} className="px-4 py-8 text-center text-text-muted">No rules configured.</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
