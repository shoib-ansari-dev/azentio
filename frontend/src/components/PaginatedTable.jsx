// Cursor-based table: parent owns data accumulation; this component renders rows + Load more
export default function PaginatedTable({ columns, data, hasMore, onLoadMore, loading = false, size, onSizeChange }) {
  return (
    <div>
      <div className="overflow-x-auto rounded-lg border border-subtle">
        <table className="w-full text-sm text-left">
          <thead className="bg-card text-text-muted uppercase text-xs tracking-wide">
            <tr>
              {columns.map((col) => (
                <th key={col.key} className="px-4 py-3 select-none">{col.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.length === 0 && !loading && (
              <tr><td colSpan={columns.length} className="px-4 py-8 text-center text-text-muted">No records found.</td></tr>
            )}
            {data.map((row, i) => (
              <tr key={row.id ?? i} className="border-t border-subtle hover:bg-card/50 transition-colors">
                {columns.map((col) => (
                  <td key={col.key} className="px-4 py-3 text-text-primary">
                    {col.render ? col.render(row[col.key], row) : row[col.key]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="flex items-center justify-between mt-3 text-sm text-text-muted">
        <div className="flex items-center gap-2">
          <span>Page size:</span>
          {[25, 50, 100].map((s) => (
            <button
              key={s}
              onClick={() => onSizeChange?.(s)}
              className={`px-2 py-0.5 rounded ${size === s ? 'bg-accent text-white' : 'hover:bg-card'}`}
            >
              {s}
            </button>
          ))}
        </div>
        {hasMore && (
          <button
            onClick={onLoadMore}
            disabled={loading}
            className="px-4 py-1.5 rounded-lg bg-card border border-subtle text-text-primary hover:border-accent text-xs disabled:opacity-50 transition-colors"
          >
            {loading ? 'Loading…' : 'Load more'}
          </button>
        )}
        {!hasMore && data.length > 0 && (
          <span className="text-xs text-text-muted">{data.length} total</span>
        )}
      </div>
    </div>
  );
}
