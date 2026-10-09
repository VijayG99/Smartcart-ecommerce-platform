import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../api';
import { ErrorBox, Loading, Notice, Pager } from '../components/Ui.jsx';

export default function AdminInventory() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [edits, setEdits] = useState({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(() => {
    api.get('/inventory', { params: { page, size: 15 } })
      .then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
  }, [page]);
  useEffect(load, [load]);

  async function save(row) {
    const quantity = Number(edits[row.productId]);
    if (!Number.isInteger(quantity) || quantity < 0) return setError('Stock must be a whole number, zero or more.');
    setError('');
    try {
      await api.put(`/inventory/${row.productId}`, { quantity });
      setNotice(`Stock for ${row.productName} set to ${quantity}.`);
      setEdits((e) => { const n = { ...e }; delete n[row.productId]; return n; });
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  if (!data) return error ? <ErrorBox message={error} /> : <Loading />;

  return (
    <div className="stack">
      <h1>Inventory</h1>
      <ErrorBox message={error} />
      <Notice message={notice} />
      <div className="table-wrap">
        <table>
          <thead><tr><th>Product</th><th>SKU</th><th>In stock</th><th>Set stock to</th></tr></thead>
          <tbody>
            {data.content.map((r) => (
              <tr key={r.productId}>
                <td>{r.productName}</td>
                <td>{r.sku}</td>
                <td><span className={`stock ${r.quantity === 0 ? 'out' : r.quantity <= 5 ? 'low' : 'ok'}`}>{r.quantity}</span></td>
                <td className="actions">
                  <input type="number" min="0" className="qty" value={edits[r.productId] ?? ''} placeholder={String(r.quantity)}
                         aria-label={`New stock for ${r.productName}`}
                         onChange={(e) => setEdits({ ...edits, [r.productId]: e.target.value })} />
                  <button className="btn small" disabled={edits[r.productId] === undefined || edits[r.productId] === ''}
                          onClick={() => save(r)}>Save</button>
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
