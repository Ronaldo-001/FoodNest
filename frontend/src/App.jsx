import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { setAuthToken } from './api/axiosInstance'
import Navbar from './components/Navbar'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import RestaurantDashboard from './pages/RestaurantDashboard'
import CustomerBrowse from './pages/CustomerBrowse'
import OrderHistory from './pages/OrderHistory'
import SurplusAlerts from './pages/SurplusAlerts'

function AppRoutes() {
  const { token, isAuthenticated, isRestaurantOwner, isCustomer } = useAuth()

  // Sync token to Axios headers whenever it changes
  React.useEffect(() => {
    setAuthToken(token)
  }, [token])

  return (
    <div className="page-layout">
      {isAuthenticated && <Navbar />}
      <main className="page-content">
        <Routes>
          <Route path="/login"    element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />} />
          <Route path="/register" element={isAuthenticated ? <Navigate to="/" replace /> : <RegisterPage />} />

          {/* Protected routes */}
          <Route path="/" element={
            !isAuthenticated ? <Navigate to="/login" replace /> :
            isRestaurantOwner ? <Navigate to="/dashboard" replace /> :
            <Navigate to="/browse" replace />
          } />
          <Route path="/dashboard" element={
            !isAuthenticated ? <Navigate to="/login" replace /> :
            !isRestaurantOwner ? <Navigate to="/browse" replace /> :
            <RestaurantDashboard />
          } />
          <Route path="/browse"  element={!isAuthenticated ? <Navigate to="/login" replace /> : <CustomerBrowse />} />
          <Route path="/orders"  element={!isAuthenticated ? <Navigate to="/login" replace /> : <OrderHistory />} />
          <Route path="/surplus" element={!isAuthenticated ? <Navigate to="/login" replace /> : <SurplusAlerts />} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
