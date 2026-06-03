import React from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { authApi } from '../api/authApi'
import toast from 'react-hot-toast'

export default function Navbar() {
  const { user, logout, isRestaurantOwner, isCustomer } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await authApi.logout()
    } catch {
      // Logout even if the API call fails
    }
    toast.success('Logged out successfully')
    logout() // clears memory + redirects to /login
  }

  return (
    <nav className="navbar">
      <div className="container">
        <div className="navbar__inner">
          <div className="navbar__brand">
            🌿 FoodWise
          </div>

          <div className="navbar__nav">
            {isRestaurantOwner && (
              <>
                <NavLink to="/dashboard" className={({ isActive }) =>
                  `navbar__link ${isActive ? 'active' : ''}`}>
                  Dashboard
                </NavLink>
                <NavLink to="/surplus" className={({ isActive }) =>
                  `navbar__link ${isActive ? 'active' : ''}`}>
                  Surplus
                </NavLink>
              </>
            )}

            {isCustomer && (
              <>
                <NavLink to="/browse" className={({ isActive }) =>
                  `navbar__link ${isActive ? 'active' : ''}`}>
                  Browse
                </NavLink>
                <NavLink to="/orders" className={({ isActive }) =>
                  `navbar__link ${isActive ? 'active' : ''}`}>
                  My Orders
                </NavLink>
                <NavLink to="/surplus" className={({ isActive }) =>
                  `navbar__link ${isActive ? 'active' : ''}`}>
                  Surplus Alerts
                </NavLink>
              </>
            )}
          </div>

          <div className="flex items-center gap-md">
            <span className="text-secondary" style={{ fontSize: '0.875rem' }}>
              {user?.username}
              {isRestaurantOwner && (
                <span className="badge badge-info" style={{ marginLeft: '0.5rem' }}>Owner</span>
              )}
            </span>
            <button
              id="navbar-logout-btn"
              className="btn btn-ghost btn-sm"
              onClick={handleLogout}>
              Logout
            </button>
          </div>
        </div>
      </div>
    </nav>
  )
}
