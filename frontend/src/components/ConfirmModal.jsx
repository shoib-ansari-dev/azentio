import { useEffect, useRef } from 'react';

export default function ConfirmModal({ title, children, onConfirm, onCancel, confirmLabel = 'Confirm', danger = false }) {
  const ref = useRef();

  useEffect(() => {
    function onKey(e) { if (e.key === 'Escape') onCancel(); }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onCancel]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60" onClick={(e) => { if (e.target === e.currentTarget) onCancel(); }}>
      <div ref={ref} className="bg-surface border border-subtle rounded-xl shadow-2xl w-full max-w-md p-6">
        <h2 className="text-lg font-semibold text-text-primary mb-4">{title}</h2>
        <div className="mb-6">{children}</div>
        <div className="flex justify-end gap-3">
          <button onClick={onCancel} className="px-4 py-2 rounded-lg border border-subtle text-text-muted hover:bg-card text-sm transition-colors">
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition-colors ${danger ? 'bg-risk-high hover:bg-red-600 text-white' : 'bg-accent hover:bg-blue-600 text-white'}`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
