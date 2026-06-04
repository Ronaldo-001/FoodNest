import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { menuApi, orderApi } from '../api/menuApi'
import { notificationApi } from '../api/inventoryApi'
import toast from 'react-hot-toast'

export default function CustomerBrowse() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [cart, setCart] = useState([])
  const [searchRestaurantId, setSearchRestaurantId] = useState('')
  const [orderNotes, setOrderNotes] = useState('')
  const [showCart, setShowCart] = useState(false)

  const restaurantIdParam = searchRestaurantId ? { restaurantId: Number(searchRestaurantId) } : {}

  const { data: menuData, isLoading } = useQuery({
    queryKey: ['menu', 'browse', searchRestaurantId],
    queryFn: () => menuApi.getAll({ ...restaurantIdParam, size: 50 }),
    placeholderData: (prev) => prev,
  })

  const createOrderMutation = useMutation({
    mutationFn: orderApi.create,
    onSuccess: () => {
      setCart([])
      setShowCart(false)
      toast.success('Order placed successfully!')
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Failed to place order'),
  })

  const subscribeMutation = useMutation({
    mutationFn: notificationApi.subscribe,
    onSuccess: () => toast.success('Subscribed to surplus alerts!'),
    onError: (err) => toast.error(err.response?.data?.message || 'Subscription failed'),
  })

  const items = menuData?.content ?? []

  const addToCart = (item) => {
    setCart(prev => {
      const existing = prev.find(c => c.menuItemId === item.id)
      if (existing) return prev.map(c => c.menuItemId === item.id ? { ...c, quantity: c.quantity + 1 } : c)
      return [...prev, { menuItemId: item.id, name: item.name, price: item.price, quantity: 1, restaurantId: item.restaurantId }]
    })
    toast.success(`${item.name} added to cart`)
  }

  const removeFromCart = (menuItemId) => setCart(prev => prev.filter(c => c.menuItemId !== menuItemId))

  const cartTotal = cart.reduce((sum, c) => sum + c.price * c.quantity, 0)

  const placeOrder = async () => {
    if (cart.length === 0) return
    const restaurantId = cart[0].restaurantId
    const payload = {
      restaurantId,
      notes: orderNotes,
      items: cart.map(c => ({ menuItemId: c.menuItemId, quantity: c.quantity })),
    }
    createOrderMutation.mutate(payload)
  }

  return (
    <div className="container">
      <div className="page-hero" style={{ paddingBottom: '1rem' }}>
        <h1 className="page-hero__title">Browse Menus</h1>
        <p className="page-hero__subtitle">Discover fresh food and surplus deals from local restaurants</p>
      </div>

      {/* Filter + Cart toggle */}
      <div className="flex items-center justify-between mb-lg" style={{ flexWrap: 'wrap', gap: '1rem' }}>
        <div className="flex items-center gap-md">
          <input
            id="restaurant-filter"
            className="form-control"
            style={{ width: '220px' }}
            type="number"
            placeholder="Filter by Restaurant ID"
            value={searchRestaurantId}
            onChange={e => setSearchRestaurantId(e.target.value)}
          />
        </div>
        <button id="view-cart-btn" className="btn btn-secondary" onClick={() => setShowCart(!showCart)}>
          🛒 Cart ({cart.length}) — ${cartTotal.toFixed(2)}
        </button>
      </div>

      {/* Cart Panel */}
      {showCart && (
        <div className="card mb-lg">
          <h3 style={{ fontWeight: '700', marginBottom: '1rem' }}>🛒 Your Cart</h3>
          {cart.length === 0 ? (
            <p className="text-muted">Your cart is empty</p>
          ) : (
            <>
              {cart.map(c => (
                <div key={c.menuItemId} className="flex justify-between items-center" style={{ padding: '0.5rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
                  <span>{c.name} × {c.quantity}</span>
                  <div className="flex items-center gap-md">
                    <span className="font-semibold">${(c.price * c.quantity).toFixed(2)}</span>
                    <button id={`remove-cart-${c.menuItemId}`} className="btn btn-ghost btn-sm" onClick={() => removeFromCart(c.menuItemId)}>✕</button>
                  </div>
                </div>
              ))}
              <div className="flex justify-between items-center mt-md" style={{ fontWeight: '700', fontSize: '1.125rem' }}>
                <span>Total</span>
                <span>${cartTotal.toFixed(2)}</span>
              </div>
              <div className="form-group mt-md">
                <label className="form-label">Order Notes</label>
                <textarea className="form-control" rows={2} value={orderNotes}
                  onChange={e => setOrderNotes(e.target.value)} placeholder="Any special requests..." />
              </div>
              <button id="place-order-btn" className="btn btn-primary btn-full mt-md"
                onClick={placeOrder} disabled={createOrderMutation.isPending}>
                {createOrderMutation.isPending ? 'Placing order...' : 'Place Order'}
              </button>
            </>
          )}
        </div>
      )}

      {/* Menu Grid */}
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '3rem' }}><div className="loading-spinner" /></div>
      ) : items.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon">🍽️</div>
          <div className="empty-state__title">No items available</div>
          <p>Try a different restaurant ID or check back later</p>
        </div>
      ) : (
        <div className="grid-auto">
          {items.map(item => (
            <div key={item.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {item.imageUrl && (
                <div style={{ borderRadius: '0.5rem', overflow: 'hidden', height: '160px', background: 'var(--bg-elevated)' }}>
                  <img src={item.imageUrl} alt={item.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                </div>
              )}
              <div>
                <div className="flex justify-between items-center">
                  <h3 style={{ fontWeight: '700', fontSize: '1.0625rem' }}>{item.name}</h3>
                  <span className="badge badge-neutral">{item.category}</span>
                </div>
                <p className="text-secondary" style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>{item.description}</p>
              </div>
              <div className="flex justify-between items-center" style={{ marginTop: 'auto' }}>
                <span style={{ fontWeight: '800', fontSize: '1.25rem', color: 'var(--color-primary-light)' }}>
                  ${item.price.toFixed(2)}
                </span>
                <div className="flex gap-sm">
                  <button
                    id={`subscribe-${item.restaurantId}`}
                    className="btn btn-ghost btn-sm"
                    title="Subscribe to surplus alerts"
                    onClick={() => subscribeMutation.mutate({ restaurantId: item.restaurantId })}>
                    🔔
                  </button>
                  <button
                    id={`add-to-cart-${item.id}`}
                    className="btn btn-primary btn-sm"
                    onClick={() => addToCart(item)}>
                    Add to Cart
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
