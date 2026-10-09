import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errorMessage, money } from '../api';
import { ErrorBox, Loading } from '../components/Ui.jsx';

export default function Checkout() {
  const navigate = useNavigate();
  const [cart, setCart] = useState(null);
  const [address, setAddress] = useState('');
  const [error, setError] = useState('');
  const [placing, setPlacing] = useState(false);

  useEffect(() => {
    api.get('/cart').then((r) => setCart(r.data)).catch((e) => setError(errorMessage(e)));
  }, []);

  async function placeOrder(e) {
    e.preventDefault();
    if (address.trim().length < 10) return setError('Enter a full shipping address (at least 10 characters).');
    setError('');
    setPlacing(true);
    try {
      await api.post('/orders', { shippingAddress: address.trim() });
      navigate('/orders', { state: { placed: true } });
    } catch (err) {
      setError(errorMessage(err));   // e.g. "Insufficient stock for product ..." - nothing was charged or changed
      setPlacing(false);
    }
  }

  if (!cart) return error ? <ErrorBox message={error} /> : <Loading />;
  if (cart.items.length === 0) return <p className="muted">Your cart is empty. <Link to="/">Browse products</Link></p>;

  return (
    <div className="two-col">
      <form className="card stack" onSubmit={placeOrder} noValidate>
        <h1>Checkout</h1>
        <ErrorBox message={error} />
        <label>Shipping address
          <textarea rows="4" value={address} onChange={(e) => setAddress(e.target.value)} maxLength={500} />
        </label>
        <p className="muted">Payment is not part of this demo. Placing the order reserves your items.</p>
        <button className="btn" disabled={placing}>{placing ? 'Placing order...' : `Place order, ${money(cart.totalAmount)}`}</button>
      </form>
      <aside className="card stack">
        <h2>Order summary</h2>
        {cart.items.map((i) => (
          <div className="line" key={i.id}><span>{i.quantity} x {i.productName}</span><span>{money(i.lineTotal)}</span></div>
        ))}
        <div className="line total"><span>Total</span><span>{money(cart.totalAmount)}</span></div>
      </aside>
    </div>
  );
}
