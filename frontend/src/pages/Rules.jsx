import { useState, useEffect } from 'react';
import api from '../api/axios';
import Navbar from '../components/Navbar';

export default function Rules() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editingParams, setEditingParams] = useState('');
  const [saving, setSaving] = useState(null);

  useEffect(() => {
    async function fetch() {
      setLoading(true);
      try {
        const { data } = await api.get('/rules');
        setRules(data);
      } finally {
        setLoading(false);
      }
    }
    fetch();
  }, []);

  async function toggleEnabled(rule) {
    setSaving(rule.id);
    try {
      const { data } = await api.put(`/rules/${rule.id}`, { enabled: !rule.enabled });
      setRules((prev) => prev.map((r) => r.id === rule.id ? data : r));
    } finally {
      setSaving(null);
    }
  }

  async function saveWeight(rule, weight) {
    try {
      const { data } = await api.put(`/rules/${rule.id}`, { riskWeight: Number(weight) });
      setRules((prev) => prev.map((r) => r.id === rule.id ? data : r));
    } catch {}
  }

  async function saveParams(rule) {
    try {
      JSON.parse(editingParams); // validate JSON before saving
      const { data } = await api.put(`/rules/${rule.id}`, { parameters: JSON.parse(editingParams) });
      setRules((prev) => prev.map((r) => r.id === rule.id ? data : r));
      setEditingId(null);
    } catch (e) {
      alert('Invalid JSON');
    }
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
                  {['Rule Code', 'Name', 'Enabled', 'Risk Weight', 'Parameters', 'Last Updated By', 'At'].map((h) => (
                    <th key={h} className="px-4 py-3">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rules.map((rule) => (
                  <tr key={rule.id} className="border-t border-subtle hover:bg-card/50 transition-colors">
                    <td className="px-4 py-3 text-text-primary font-mono text-xs">{rule.code}</td>
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
                    <td className="px-4 py-3">
                      <input
                        type="number"
                        defaultValue={rule.riskWeight}
                        onBlur={(e) => saveWeight(rule, e.target.value)}
                        className="bg-card border border-subtle rounded px-2 py-1 text-sm text-text-primary focus:outline-none focus:border-accent w-20"
                      />
                    </td>
                    <td className="px-4 py-3">
                      {editingId === rule.id ? (
                        <div className="flex flex-col gap-1">
                          <textarea
                            value={editingParams}
                            onChange={(e) => setEditingParams(e.target.value)}
                            className="bg-card border border-accent rounded px-2 py-1 text-xs text-text-primary font-mono focus:outline-none resize-none h-20 w-64"
                          />
                          <div className="flex gap-1">
                            <button onClick={() => saveParams(rule)} className="px-2 py-0.5 text-xs bg-accent text-white rounded">Save</button>
                            <button onClick={() => setEditingId(null)} className="px-2 py-0.5 text-xs bg-subtle text-text-muted rounded">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <button
                          onClick={() => { setEditingId(rule.id); setEditingParams(JSON.stringify(rule.parameters || {}, null, 2)); }}
                          className="px-2 py-1 text-xs bg-card border border-subtle rounded text-text-muted hover:text-text-primary hover:border-accent transition-colors"
                        >
                          Edit
                        </button>
                      )}
                    </td>
                    <td className="px-4 py-3 text-text-muted">{rule.lastUpdatedBy || '—'}</td>
                    <td className="px-4 py-3 text-text-muted">{rule.lastUpdatedAt ? new Date(rule.lastUpdatedAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
                {!rules.length && <tr><td colSpan={7} className="px-4 py-8 text-center text-text-muted">No rules configured.</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
