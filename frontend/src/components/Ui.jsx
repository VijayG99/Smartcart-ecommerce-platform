export const Loading = ({ text = 'Loading...' }) => <p className="muted" role="status">{text}</p>;

export const ErrorBox = ({ message }) =>
  message ? <p className="alert error" role="alert">{message}</p> : null;

export const Notice = ({ message }) =>
  message ? <p className="alert ok" role="status">{message}</p> : null;

export function Pager({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="pager">
      <button className="btn ghost small" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button className="btn ghost small" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
    </div>
  );
}

export const StatusBadge = ({ status }) => <span className={`badge ${status.toLowerCase()}`}>{status}</span>;
