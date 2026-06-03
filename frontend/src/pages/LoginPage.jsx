import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { authApi } from '../api/authApi'
import toast from 'react-hot-toast'

export default function LoginPage() {
  const { login } = useAuth()
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
    if (error) setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await authApi.login(form)
      login(data)
      toast.success(`Welcome back, ${data.username}!`)
    } catch (err) {
      const msg = err.response?.data?.message || 'Login failed. Please try again.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="card auth-card">
        <div className="auth-card__header">
          <div style={{ fontSize: '3rem', marginBottom: '0.5rem' }}>🌿</div>
          <h1 className="auth-card__title">Welcome to FoodWise</h1>
          <p className="auth-card__subtitle">Sign in to manage your restaurant or browse menus</p>
        </div>

        <form className="auth-card__form" onSubmit={handleSubmit} id="login-form">
          <div className="form-group">
            <label htmlFor="usernameOrEmail" className="form-label">Username or Email</label>
            <input
              id="usernameOrEmail"
              name="usernameOrEmail"
              type="text"
              className="form-control"
              placeholder="jane@example.com"
              value={form.usernameOrEmail}
              onChange={handleChange}
              autoComplete="username"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password" className="form-label">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              className="form-control"
              placeholder="••••••••"
              value={form.password}
              onChange={handleChange}
              autoComplete="current-password"
              required
            />
          </div>

          {error && <p className="form-error">{error}</p>}

          <button
            id="login-submit-btn"
            type="submit"
            className="btn btn-primary btn-full btn-lg"
            disabled={loading}>
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className="auth-card__footer">
          Don't have an account?{' '}
          <Link to="/register">Create one</Link>
        </div>
      </div>
    </div>
  )
}
