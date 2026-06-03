import React from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { inventoryApi, notificationApi } from '../api/inventoryApi'
import toast from 'react-hot-toast'

export default function SurplusAlerts() {
  const { isRestaurantOwner, isCustomer } = useAuth()

  // Restaurant owner sees their own surplus items
  const { data: surplusItems = [], isLoading: surplusLoading } = useQuery({
    queryKey: ['surplus'],
    queryFn: inventoryApi.getSurplus,
    enabled: isRestaurantOwner,
    refetchInterval: 30_000,
  })

  // Customer sees their subscriptions
  const { data: subscriptions = [], isLoading: subLoading } = useQuery({
    queryKey: ['subscriptions'],
    queryFn: notificationApi.getHistory,
    enabled: isCustomer,
  })

  const [restaurantId, setRestaurantId] = React.useState('')

  const subscribeMutation = useMutation({
    mutationFn: notificationApi.subscribe,
    onSuccess: () => toast.success('Subscribed to alerts!'),
    onError: (err) => toast.error(err.response?.data?.message || 'Subscription failed'),
  })

  return (
    <div className="container">
      <div className="page-hero" style={{ paddingBottom: '1rem' }}>
        <h1 className="page-hero__title">
          {isRestaurantOwner ? '⚠️ Surplus Overview' : '🔔 Surplus Alerts'}
        </h1>
        <p className="page-hero__subtitle">
          {isRestaurantOwner
            ? 'Items that are low on stock or expiring soon'
            : 'Subscribe to restaurants and get notified about surplus food deals'}
        </p>
      </div>

      {/* Restaurant Owner View */}
      {isRestaurantOwner && (
        <>
          {surplusLoading ? (
            <div style={{ textAlign: 'center', padding: '3rem' }}><div className="loading-spinner" /></div>
          ) : surplusItems.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state__icon">✅</div>
              <div className="empty-state__title">No surplus items</div>
              <p>Your inventory is in good shape!</p>
            </div>
          ) : (
            <div className="grid-auto">
              {surplusItems.map(item => (
                <div key={item.id} className="card surplus-card">
                  <div className="flex justify-between items-center mb-md">
                    <h3 style={{ fontWeight: '700' }}>{item.name}</h3>
                    <span className="badge badge-warning surplus-badge">⚠️ Surplus</span>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem', fontSize: '0.9rem' }}>
                    <div className="flex justify-between">
                      <span className="text-muted">Current Qty</span>
                      <span style={{ fontWeight: '600', color: item.quantity < item.threshold ? 'var(--color-danger-light)' : '' }}>
                        {item.quantity} {item.unit || 'units'}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted">Threshold</span>
                      <span>{item.threshold} {item.unit || 'units'}</span>
                    </div>
                    {item.expiryDate && (
                      <div className="flex justify-between">
                        <span className="text-muted">Expires</span>
                        <span style={{ color: 'var(--color-accent)' }}>{item.expiryDate}</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* Customer View */}
      {isCustomer && (
        <>
          <div className="card mb-lg">
            <h3 style={{ fontWeight: '700', marginBottom: '1rem' }}>Subscribe to a Restaurant</h3>
            <p className="text-secondary" style={{ marginBottom: '1rem', fontSize: '0.9rem' }}>
              Get email notifications when your favorite restaurant has surplus food available at reduced prices.
            </p>
            <div className="flex gap-md" style={{ flexWrap: 'wrap' }}>
              <input
                id="subscribe-restaurant-id"
                className="form-control"
                style={{ width: '200px' }}
                type="number"
                placeholder="Restaurant ID"
                value={restaurantId}
                onChange={e => setRestaurantId(e.target.value)}
              />
              <button
                id="subscribe-submit-btn"
                className="btn btn-primary"
                disabled={!restaurantId || subscribeMutation.isPending}
                onClick={() => subscribeMutation.mutate({ restaurantId: Number(restaurantId) })}>
                {subscribeMutation.isPending ? 'Subscribing...' : '🔔 Subscribe'}
              </button>
            </div>
          </div>

          <h3 style={{ fontWeight: '700', marginBottom: '1rem' }}>Your Subscriptions</h3>
          {subLoading ? (
            <div className="loading-spinner" />
          ) : subscriptions.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state__icon">🔔</div>
              <div className="empty-state__title">No subscriptions yet</div>
              <p>Enter a restaurant ID above to subscribe to their surplus alerts</p>
            </div>
          ) : (
            <div className="grid-3">
              {subscriptions.map(sub => (
                <div key={sub.id} className="card">
                  <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>🏪</div>
                  <h4 style={{ fontWeight: '700' }}>Restaurant #{sub.restaurantId}</h4>
                  <p className="text-muted" style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>
                    Alerts sent to: {sub.customerEmail}
                  </p>
                  <p className="text-muted" style={{ fontSize: '0.8125rem' }}>
                    Since {new Date(sub.createdAt).toLocaleDateString()}
                  </p>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  )
}
