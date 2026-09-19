import { useState, useEffect } from 'react';

const listeners = new Set();
export function emitError(msg) { listeners.forEach((fn) => fn(msg)); }

export default function GlobalErrorBanner() {
  const [message, setMessage] = useState(null);

  useEffect(() => {
    function handler(msg) { setMessage(msg); }
    listeners.add(handler);
    return () => listeners.delete(handler);
  }, []);

  useEffect(() => {
    if (!message) return;
    const t = setTimeout(() => setMessage(null), 5000);
    return () => clearTimeout(t);
  }, [message]);

  if (!message) return null;

  return (
    <div className="fixed top-0 left-0 right-0 z-50 bg-risk-high text-white text-sm px-4 py-2 flex justify-between items-center">
      <span>{message}</span>
      <button onClick={() => setMessage(null)} className="ml-4 font-bold hover:opacity-70">✕</button>
    </div>
  );
}
