export default function RiskScoreBadge({ score }) {
  const color =
    score >= 70 ? 'bg-risk-high text-white' :
    score >= 40 ? 'bg-risk-medium text-black' :
                  'bg-risk-low text-black';
  return (
    <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-bold ${color}`}>
      {score}
    </span>
  );
}
