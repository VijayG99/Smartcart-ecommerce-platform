import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';

export default function Navbar() {
  const { user, logout, isAdmin, isCustomer } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="nav">
      <Link to="/" className="brand">SmartCart</Link>
      <nav>
        <NavLink to="/" end>Shop</NavLink>
        {isCustomer && <NavLink to="/cart">Cart</NavLink>}
        {isCustomer && <NavLink to="/orders">My orders</NavLink>}
        {isAdmin && <NavLink to="/admin/products">Products</NavLink>}
        {isAdmin && <NavLink to="/admin/inventory">Inventory</NavLink>}
        {isAdmin && <NavLink to="/admin/orders">Orders</NavLink>}
      </nav>
      <div className="nav-user">
        {user ? (
          <>
            <NavLink to="/profile">{user.name}</NavLink>
            <button className="link" onClick={() => { logout(); navigate('/login'); }}>Log out</button>
          </>
        ) : (
          <>
            <NavLink to="/login">Log in</NavLink>
            <NavLink to="/register" className="btn small">Create account</NavLink>
          </>
        )}
      </div>
    </header>
  );
}
