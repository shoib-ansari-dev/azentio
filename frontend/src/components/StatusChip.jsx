const ALERT_COLORS = {
  OPEN: 'bg-accent/20 text-accent',
  ACKNOWLEDGED: 'bg-yellow-500/20 text-yellow-400',
  DISMISSED: 'bg-gray-500/20 text-gray-400',
  ESCALATED: 'bg-purple-500/20 text-purple-400',
};

const CASE_COLORS = {
  OPEN: 'bg-accent/20 text-accent',
  IN_REVIEW: 'bg-yellow-500/20 text-yellow-400',
  CLOSED: 'bg-gray-500/20 text-gray-400',
  REPORTED: 'bg-green-500/20 text-green-400',
};

const PRIORITY_COLORS = {
  LOW: 'bg-green-500/20 text-green-400',
  MEDIUM: 'bg-yellow-500/20 text-yellow-400',
  HIGH: 'bg-orange-500/20 text-orange-400',
  CRITICAL: 'bg-risk-high/20 text-risk-high',
};

export default function StatusChip({ value, type = 'alert' }) {
  const map = type === 'priority' ? PRIORITY_COLORS : type === 'case' ? CASE_COLORS : ALERT_COLORS;
  const cls = map[value] || 'bg-subtle text-text-muted';
  return (
    <span className={`inline-block px-2 py-0.5 rounded text-xs font-semibold uppercase tracking-wide ${cls}`}>
      {value}
    </span>
  );
}
