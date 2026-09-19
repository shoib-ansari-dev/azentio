import { useEffect, useRef } from 'react';

export default function ConfirmModal({ title, children, onConfirm, onCancel, confirmLabel = 'Confirm', danger = false, submitting = false }) {
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
          <button onClick={onCancel} disabled={submitting} className="px-4 py-2 rounded-lg border border-subtle text-text-muted hover:bg-card text-sm transition-colors disabled:opacity-50">
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={submitting}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition-colors disabled:opacity-60 disabled:cursor-not-allowed ${danger ? 'bg-risk-high hover:bg-red-600 text-white' : 'bg-accent hover:bg-blue-600 text-white'}`}
          >
            {submitting ? 'Please wait…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
