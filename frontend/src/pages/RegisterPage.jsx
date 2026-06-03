import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { authApi } from '../api/authApi'
import toast from 'react-hot-toast'

const ROLES = [
  { value: 'CUSTOMER', label: 'Customer — browse and order food' },
  { value: 'RESTAURANT_OWNER', label: 'Restaurant Owner — manage menu and inventory' },
]

export default function RegisterPage() {
  const { login } = useAuth()
  const [form, setForm] = useState({
    username: '', email: '', password: '', confirmPassword: '',
    role: 'CUSTOMER', restaurantId: '',
  })
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState({})

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
    if (errors[name]) setErrors(prev => ({ ...prev, [name]: '' }))
  }

  const validate = () => {
    const errs = {}
    if (!form.username) errs.username = 'Username is required'
    if (!/^[a-zA-Z0-9_-]{3,50}$/.test(form.username))
      errs.username = 'Username must be 3-50 alphanumeric characters'
    if (!form.email) errs.email = 'Email is required'
    if (form.password.length < 8) errs.password = 'Password must be at least 8 characters'
    if (form.password.length > 128) errs.password = 'Password must be 128 characters or less'
    if (form.password !== form.confirmPassword) errs.confirmPassword = 'Passwords do not match'
    if (form.role === 'RESTAURANT_OWNER' && !form.restaurantId)
      errs.restaurantId = 'Restaurant ID is required for owners'
    return errs
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }

    setLoading(true)
    try {
      const payload = {
        username: form.username,
        email:    form.email,
        password: form.password,
        role:     form.role,
        ...(form.role === 'RESTAURANT_OWNER' && { restaurantId: Number(form.restaurantId) }),
      }
      const data = await authApi.register(payload)
      login(data)
      toast.success(`Welcome to FoodWise, ${data.username}!`)
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed.'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="card auth-card" style={{ maxWidth: '480px' }}>
        <div className="auth-card__header">
          <div style={{ fontSize: '3rem', marginBottom: '0.5rem' }}>🌿</div>
          <h1 className="auth-card__title">Create Account</h1>
          <p className="auth-card__subtitle">Join FoodWise to reduce food waste</p>
        </div>

        <form className="auth-card__form" onSubmit={handleSubmit} id="register-form">
          <div className="form-group">
            <label htmlFor="username" className="form-label">Username</label>
            <input id="username" name="username" type="text" className="form-control"
              placeholder="jane_smith" value={form.username} onChange={handleChange} required />
            {errors.username && <span className="form-error">{errors.username}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="reg-email" className="form-label">Email</label>
            <input id="reg-email" name="email" type="email" className="form-control"
              placeholder="jane@example.com" value={form.email} onChange={handleChange}
              autoComplete="email" required />
            {errors.email && <span className="form-error">{errors.email}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="reg-password" className="form-label">Password</label>
            <input id="reg-password" name="password" type="password" className="form-control"
              placeholder="8+ characters" value={form.password} onChange={handleChange}
              autoComplete="new-password" required />
            {errors.password && <span className="form-error">{errors.password}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword" className="form-label">Confirm Password</label>
            <input id="confirmPassword" name="confirmPassword" type="password" className="form-control"
              placeholder="Re-enter password" value={form.confirmPassword} onChange={handleChange}
              autoComplete="new-password" required />
            {errors.confirmPassword && <span className="form-error">{errors.confirmPassword}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="role" className="form-label">I am a...</label>
            <select id="role" name="role" className="form-control" value={form.role} onChange={handleChange}>
              {ROLES.map(r => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
          </div>

          {form.role === 'RESTAURANT_OWNER' && (
            <div className="form-group">
              <label htmlFor="restaurantId" className="form-label">Restaurant ID</label>
              <input id="restaurantId" name="restaurantId" type="number" className="form-control"
                placeholder="Your restaurant's ID" value={form.restaurantId} onChange={handleChange} />
              {errors.restaurantId && <span className="form-error">{errors.restaurantId}</span>}
            </div>
          )}

          <button id="register-submit-btn" type="submit"
            className="btn btn-primary btn-full btn-lg" disabled={loading}>
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        <div className="auth-card__footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  )
}
