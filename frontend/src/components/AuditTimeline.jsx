import { useState } from 'react';

export default function AuditTimeline({ entries = [] }) {
  const [open, setOpen] = useState(false);

  return (
    <div className="border border-subtle rounded-lg mt-4">
      <button
        onClick={() => setOpen((p) => !p)}
        className="w-full flex justify-between items-center px-4 py-3 text-sm text-text-primary hover:bg-card transition-colors"
      >
        <span className="font-semibold">Audit Trail ({entries.length})</span>
        <span>{open ? '▲' : '▼'}</span>
      </button>
      {open && (
        <ul className="px-4 pb-4 space-y-3">
          {entries.map((e, i) => (
            <li key={i} className="flex gap-3 text-sm">
              <span className="mt-1 w-2 h-2 rounded-full bg-accent flex-shrink-0" />
              <div>
                <span className="text-text-muted">{new Date(e.timestamp).toLocaleString()}</span>
                <span className="text-text-primary mx-2 font-medium">{e.actor}</span>
                <span className="text-text-muted">{e.from} → {e.to}</span>
              </div>
            </li>
          ))}
          {entries.length === 0 && <li className="text-text-muted text-sm">No history yet.</li>}
        </ul>
      )}
    </div>
  );
}
