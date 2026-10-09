import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Products from './pages/Products.jsx';
import ProductDetails from './pages/ProductDetails.jsx';
import Cart from './pages/Cart.jsx';
import Checkout from './pages/Checkout.jsx';
import Orders from './pages/Orders.jsx';
import Profile from './pages/Profile.jsx';
import AdminProducts from './pages/AdminProducts.jsx';
import AdminInventory from './pages/AdminInventory.jsx';
import AdminOrders from './pages/AdminOrders.jsx';

export default function App() {
  return (
    <>
      <Navbar />
      <main className="page">
        <Routes>
          <Route path="/" element={<Products />} />
          <Route path="/products/:id" element={<ProductDetails />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route path="/cart" element={<ProtectedRoute roles={['CUSTOMER']}><Cart /></ProtectedRoute>} />
          <Route path="/checkout" element={<ProtectedRoute roles={['CUSTOMER']}><Checkout /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute roles={['CUSTOMER']}><Orders /></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />

          <Route path="/admin/products" element={<ProtectedRoute roles={['ADMIN']}><AdminProducts /></ProtectedRoute>} />
          <Route path="/admin/inventory" element={<ProtectedRoute roles={['ADMIN']}><AdminInventory /></ProtectedRoute>} />
          <Route path="/admin/orders" element={<ProtectedRoute roles={['ADMIN']}><AdminOrders /></ProtectedRoute>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
