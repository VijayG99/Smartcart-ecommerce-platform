import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errorMessage, money } from '../api';
import { useAuth } from '../AuthContext.jsx';
import { ErrorBox, Loading, Notice, Pager } from '../components/Ui.jsx';

export function AddToCartButton({ product, quantity = 1, onAdded }) {
  const { user, isCustomer } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  if (user && !isCustomer) return null;   // admins do not shop
  const soldOut = product.availableQuantity <= 0;

  async function add() {
    if (!user) return navigate('/login');
    setBusy(true);
    setError('');
    try {
      await api.post('/cart/items', { productId: product.id, quantity });
      onAdded?.(`${product.name} added to your cart.`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button className="btn small" onClick={add} disabled={busy || soldOut}>
        {soldOut ? 'Sold out' : busy ? 'Adding...' : 'Add to cart'}
      </button>
      {error && <span className="field-error">{error}</span>}
    </>
  );
}

export const StockLabel = ({ qty }) =>
  qty <= 0 ? <span className="stock out">Out of stock</span>
    : qty <= 5 ? <span className="stock low">Only {qty} left</span>
    : <span className="stock ok">In stock</span>;

function ProductCard({ p, onAdded }) {
  return (
    <article className="product">
      <Link to={`/products/${p.id}`} className="thumb" aria-label={p.name}>{p.name.slice(0, 2)}</Link>
      <div className="product-body">
        <span className="muted small-text">{p.categoryName}</span>
        <h3><Link to={`/products/${p.id}`}>{p.name}</Link></h3>
        <StockLabel qty={p.availableQuantity} />
      </div>
      <div className="product-foot">
        <strong className="price">{money(p.price)}</strong>
        <AddToCartButton product={p} onAdded={onAdded} />
      </div>
    </article>
  );
}

export default function Products() {
  const [filters, setFilters] = useState({ q: '', categoryId: '', minPrice: '', maxPrice: '', inStock: false, sort: 'createdAt,desc' });
  const [applied, setApplied] = useState(filters);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [categories, setCategories] = useState([]);
  const [smartText, setSmartText] = useState('');
  const [smart, setSmart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    api.get('/categories').then((r) => setCategories(r.data)).catch(() => {});
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    const [sortBy, direction] = applied.sort.split(',');
    const params = { page, size: 12, sortBy, direction };
    if (applied.q) params.q = applied.q;
    if (applied.categoryId) params.categoryId = applied.categoryId;
    if (applied.minPrice) params.minPrice = applied.minPrice;
    if (applied.maxPrice) params.maxPrice = applied.maxPrice;
    if (applied.inStock) params.inStock = true;
    api.get('/products', { params })
      .then((r) => !cancelled && setData(r.data))
      .catch((e) => !cancelled && setError(errorMessage(e)))
      .finally(() => !cancelled && setLoading(false));
    return () => { cancelled = true; };
  }, [applied, page]);

  const set = (k) => (e) => setFilters({ ...filters, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  function apply(e) {
    e.preventDefault();
    setSmart(null);
    setPage(0);
    setApplied(filters);
  }

  async function runSmartSearch(e) {
    e.preventDefault();
    if (!smartText.trim()) return;
    setError('');
    try {
      const { data } = await api.get('/products/smart-search', { params: { query: smartText } });
      setSmart(data);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  const products = smart ? smart.results : data?.content ?? [];

  return (
    <div className="stack">
      <h1>Shop</h1>
      <form className="smart" onSubmit={runSmartSearch}>
        <input value={smartText} onChange={(e) => setSmartText(e.target.value)}
               placeholder="Describe what you need, e.g. affordable wireless headphones for office use" aria-label="Describe what you need" />
        <button className="btn small">Smart search</button>
        {smart && <button type="button" className="btn ghost small" onClick={() => setSmart(null)}>Clear</button>}
      </form>
      {smart && (
        <p className="muted">
          Looking for <strong>{smart.keywords.join(', ') || 'nothing recognisable'}</strong>
          {smart.maxPrice && <> under {money(smart.maxPrice)}</>}
          {smart.minPrice && <> above {money(smart.minPrice)}</>}
          {smart.cheapFirst && <>, cheapest first</>}. Matched by keywords, not machine learning.
        </p>
      )}

      <form className="filters" onSubmit={apply}>
        <input placeholder="Search products" value={filters.q} onChange={set('q')} aria-label="Search products" />
        <select value={filters.categoryId} onChange={set('categoryId')} aria-label="Category">
          <option value="">All categories</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input type="number" min="0" placeholder="Min price" value={filters.minPrice} onChange={set('minPrice')} aria-label="Minimum price" />
        <input type="number" min="0" placeholder="Max price" value={filters.maxPrice} onChange={set('maxPrice')} aria-label="Maximum price" />
        <select value={filters.sort} onChange={set('sort')} aria-label="Sort">
          <option value="createdAt,desc">Newest</option>
          <option value="price,asc">Price: low to high</option>
          <option value="price,desc">Price: high to low</option>
          <option value="name,asc">Name A-Z</option>
        </select>
        <label className="inline"><input type="checkbox" checked={filters.inStock} onChange={set('inStock')} /> In stock only</label>
        <button className="btn small">Apply</button>
      </form>

      <ErrorBox message={error} />
      <Notice message={notice} />
      {loading && !smart ? <Loading /> : (
        products.length === 0
          ? <p className="muted">No products match. Try removing a filter.</p>
          : <div className="grid">{products.map((p) => <ProductCard key={p.id} p={p} onAdded={setNotice} />)}</div>
      )}
      {!smart && <Pager page={page} totalPages={data?.totalPages} onChange={setPage} />}
    </div>
  );
}
