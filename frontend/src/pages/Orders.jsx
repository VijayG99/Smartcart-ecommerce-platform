import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import api, { dateTime, errorMessage, money } from '../api';
import { useAuth } from '../AuthContext.jsx';
import { ErrorBox, Loading, Notice, Pager, StatusBadge } from '../components/Ui.jsx';

export default function Orders() {
  const { user } = useAuth();
  const location = useLocation();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const polls = useRef(0);

  const load = useCallback(async () => {
    try {
      const { data } = await api.get(`/orders/user/${user.id}`, { params: { page, size: 5 } });
      setData(data);
      return data;
    } catch (err) {
      setError(errorMessage(err));
      return null;
    }
  }, [user.id, page]);

  // Orders start as PENDING and become CONFIRMED a moment later (asynchronous Kafka processing).
  // Poll a few times so the customer sees the change without reloading.
  useEffect(() => {
    polls.current = 0;
    let timer;
    const tick = async () => {
      const result = await load();
      if (result?.content.some((o) => o.status === 'PENDING') && polls.current++ < 6) timer = setTimeout(tick, 2500);
    };
    tick();
    return () => clearTimeout(timer);
  }, [load]);

  if (!data) return error ? <ErrorBox message={error} /> : <Loading />;

  return (
    <div className="stack">
      <h1>My orders</h1>
      <Notice message={location.state?.placed ? 'Order placed. We are confirming it now.' : ''} />
      <ErrorBox message={error} />
      {data.content.length === 0 && <p className="muted">You have not placed any orders yet.</p>}
      {data.content.map((o) => (
        <article className="card stack" key={o.id}>
          <div className="between">
            <strong>Order #{o.id}</strong>
            <StatusBadge status={o.status} />
          </div>
          <span className="muted">{dateTime(o.createdAt)}</span>
          {o.items.map((i, idx) => (
            <div className="line" key={idx}><span>{i.quantity} x {i.productName}</span><span>{money(i.lineTotal)}</span></div>
          ))}
          <div className="line total"><span>Total</span><span>{money(o.totalAmount)}</span></div>
          <span className="muted">Ships to: {o.shippingAddress}</span>
        </article>
      ))}
      <Pager page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  );
}
