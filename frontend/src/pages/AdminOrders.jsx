import { useCallback, useEffect, useState } from 'react';
import api, { dateTime, errorMessage, money } from '../api';
import { ErrorBox, Loading, Notice, Pager, StatusBadge } from '../components/Ui.jsx';

// Mirrors the backend state machine (OrderStatus.canTransitionTo); the server is still the authority.
const NEXT = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
};

export default function AdminOrders() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(() => {
    api.get('/orders', { params: { page, size: 10 } }).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
    api.get('/analytics/summary').then((r) => setSummary(r.data)).catch(() => {});
  }, [page]);
  useEffect(load, [load]);

  async function changeStatus(order, status) {
    setError('');
    try {
      await api.put(`/orders/${order.id}/status`, { status });
      setNotice(`Order #${order.id} is now ${status}.`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  if (!data) return error ? <ErrorBox message={error} /> : <Loading />;

  return (
    <div className="stack">
      <h1>Orders</h1>
      {summary && (
        <div className="stats">
          <div className="card"><span className="muted">Orders processed</span><strong className="price large">{summary.totalOrders}</strong></div>
          <div className="card"><span className="muted">Revenue</span><strong className="price large">{money(summary.totalRevenue)}</strong></div>
          <p className="muted">Figures come from the analytics Kafka consumer, so they can lag a moment behind new orders.</p>
        </div>
      )}
      <ErrorBox message={error} />
      <Notice message={notice} />
      <div className="table-wrap">
        <table>
          <thead><tr><th>Order</th><th>Placed</th><th>Customer</th><th>Total</th><th>Status</th><th>Change to</th></tr></thead>
          <tbody>
            {data.content.map((o) => (
              <tr key={o.id}>
                <td>#{o.id}</td><td>{dateTime(o.createdAt)}</td><td>User {o.userId}</td><td>{money(o.totalAmount)}</td>
                <td><StatusBadge status={o.status} /></td>
                <td>
                  {NEXT[o.status].length === 0 ? <span className="muted">Final</span> : (
                    <select value="" aria-label={`Change status of order ${o.id}`} onChange={(e) => e.target.value && changeStatus(o, e.target.value)}>
                      <option value="">Select...</option>
                      {NEXT[o.status].map((s) => <option key={s} value={s}>{s}</option>)}
                    </select>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  );
}
