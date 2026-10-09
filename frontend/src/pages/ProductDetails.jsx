import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage, money } from '../api';
import { ErrorBox, Loading, Notice } from '../components/Ui.jsx';
import { AddToCartButton, StockLabel } from './Products.jsx';

export default function ProductDetails() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    setProduct(null);
    setError('');
    api.get(`/products/${id}`).then((r) => setProduct(r.data)).catch((e) => setError(errorMessage(e)));
  }, [id]);

  if (error) return <div className="stack"><ErrorBox message={error} /><Link to="/">Back to shop</Link></div>;
  if (!product) return <Loading />;

  return (
    <div className="detail card">
      <div className="thumb big" aria-hidden="true">{product.name.slice(0, 2)}</div>
      <div className="stack">
        <Link to="/" className="muted">Back to shop</Link>
        <span className="muted">{product.categoryName} / SKU {product.sku}</span>
        <h1>{product.name}</h1>
        <p>{product.description || 'No description provided.'}</p>
        <strong className="price large">{money(product.price)}</strong>
        <StockLabel qty={product.availableQuantity} />
        <div className="row">
          <input type="number" min="1" max={Math.max(product.availableQuantity, 1)} value={quantity}
                 onChange={(e) => setQuantity(Math.max(1, Number(e.target.value) || 1))} aria-label="Quantity" className="qty" />
          <AddToCartButton product={product} quantity={quantity} onAdded={setNotice} />
        </div>
        <Notice message={notice} />
      </div>
    </div>
  );
}
