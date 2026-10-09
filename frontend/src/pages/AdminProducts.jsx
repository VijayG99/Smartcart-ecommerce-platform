import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage, money } from '../api';
import { ErrorBox, Loading, Notice, Pager } from '../components/Ui.jsx';

const EMPTY = { name: '', description: '', price: '', sku: '', categoryId: '', initialQuantity: 0 };

export default function AdminProducts() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const [newCategory, setNewCategory] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    api.get('/products', { params: { page, size: 10, sortBy: 'name', direction: 'asc' } })
      .then((r) => setData(r.data)).catch((e) => setError(errorMessage(e)));
  }, [page]);
  const loadCategories = () => api.get('/categories').then((r) => setCategories(r.data)).catch(() => {});

  useEffect(load, [load]);
  useEffect(() => { loadCategories(); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  function validate() {
    if (!form.name.trim()) return 'Name is required.';
    if (!form.sku.trim()) return 'SKU is required.';
    if (!(Number(form.price) > 0)) return 'Price must be greater than zero.';
    if (!form.categoryId) return 'Choose a category.';
    if (!editingId && Number(form.initialQuantity) < 0) return 'Stock cannot be negative.';
    return '';
  }

  async function save(e) {
    e.preventDefault();
    const problem = validate();
    if (problem) return setError(problem);
    setSaving(true);
    setError('');
    const body = {
      name: form.name, description: form.description, price: form.price, sku: form.sku,
      categoryId: Number(form.categoryId),
      initialQuantity: editingId ? null : Number(form.initialQuantity),
    };
    try {
      if (editingId) await api.put(`/products/${editingId}`, body);
      else await api.post('/products', body);
      setNotice(editingId ? 'Product updated.' : 'Product created.');
      setForm(EMPTY);
      setEditingId(null);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  function edit(p) {
    setEditingId(p.id);
    setForm({ name: p.name, description: p.description || '', price: p.price, sku: p.sku, categoryId: p.categoryId, initialQuantity: 0 });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function deactivate(p) {
    if (!window.confirm(`Remove "${p.name}" from the shop?`)) return;
    try {
      await api.delete(`/products/${p.id}`);
      setNotice('Product removed from the shop.');
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function addCategory(e) {
    e.preventDefault();
    if (!newCategory.trim()) return;
    try {
      await api.post('/categories', { name: newCategory.trim() });
      setNewCategory('');
      loadCategories();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="stack">
      <h1>Manage products</h1>
      <ErrorBox message={error} />
      <Notice message={notice} />

      <form className="card stack" onSubmit={save} noValidate>
        <h2>{editingId ? `Edit product #${editingId}` : 'New product'}</h2>
        <div className="form-grid">
          <label>Name<input value={form.name} onChange={set('name')} /></label>
          <label>SKU<input value={form.sku} onChange={set('sku')} /></label>
          <label>Price (INR)<input type="number" min="0" step="0.01" value={form.price} onChange={set('price')} /></label>
          <label>Category
            <select value={form.categoryId} onChange={set('categoryId')}>
              <option value="">Select...</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </label>
          {!editingId && <label>Opening stock<input type="number" min="0" value={form.initialQuantity} onChange={set('initialQuantity')} /></label>}
        </div>
        <label>Description<textarea rows="3" value={form.description} onChange={set('description')} maxLength={2000} /></label>
        <div className="row">
          <button className="btn" disabled={saving}>{saving ? 'Saving...' : editingId ? 'Save changes' : 'Create product'}</button>
          {editingId && <button type="button" className="btn ghost" onClick={() => { setEditingId(null); setForm(EMPTY); }}>Cancel</button>}
        </div>
      </form>

      <form className="row" onSubmit={addCategory}>
        <input placeholder="New category name" value={newCategory} onChange={(e) => setNewCategory(e.target.value)} aria-label="New category name" />
        <button className="btn ghost small">Add category</button>
      </form>

      {!data ? <Loading /> : (
        <>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Name</th><th>SKU</th><th>Category</th><th>Price</th><th>Stock</th><th></th></tr></thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id}>
                    <td>{p.name}</td><td>{p.sku}</td><td>{p.categoryName}</td><td>{money(p.price)}</td><td>{p.availableQuantity}</td>
                    <td className="actions">
                      <button className="link" onClick={() => edit(p)}>Edit</button>
                      <button className="link danger" onClick={() => deactivate(p)}>Remove</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
