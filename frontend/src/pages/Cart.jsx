import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage, money } from '../api';
import { ErrorBox, Loading } from '../components/Ui.jsx';

export default function Cart() {
  const [cart, setCart] = useState(null);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    api.get('/cart').then((r) => setCart(r.data)).catch((e) => setError(errorMessage(e)));
  }, []);

  async function run(itemId, request) {
    setBusyId(itemId);
    setError('');
    try {
      const { data } = await request();
      setCart(data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  if (!cart) return error ? <ErrorBox message={error} /> : <Loading />;

  return (
    <div className="stack">
      <h1>Your cart</h1>
      <ErrorBox message={error} />
      {cart.items.length === 0 ? (
        <p className="muted">Your cart is empty. <Link to="/">Browse products</Link></p>
      ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Product</th><th>Price</th><th>Quantity</th><th>Subtotal</th><th></th></tr></thead>
              <tbody>
                {cart.items.map((i) => (
                  <tr key={i.id}>
                    <td><Link to={`/products/${i.productId}`}>{i.productName}</Link></td>
                    <td>{money(i.unitPrice)}</td>
                    <td>
                      <input type="number" min="1" max={i.availableQuantity} defaultValue={i.quantity} className="qty"
                             disabled={busyId === i.id} aria-label={`Quantity for ${i.productName}`}
                             onBlur={(e) => {
                               const q = Number(e.target.value);
                               if (q >= 1 && q !== i.quantity) run(i.id, () => api.put(`/cart/items/${i.id}`, { quantity: q }));
                             }} />
                    </td>
                    <td>{money(i.lineTotal)}</td>
                    <td><button className="link danger" disabled={busyId === i.id}
                                onClick={() => run(i.id, () => api.delete(`/cart/items/${i.id}`))}>Remove</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="summary">
            <span>{cart.totalItems} item(s)</span>
            <strong className="price large">{money(cart.totalAmount)}</strong>
            <Link to="/checkout" className="btn">Go to checkout</Link>
          </div>
        </>
      )}
    </div>
  );
}
