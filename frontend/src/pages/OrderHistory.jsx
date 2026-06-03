import React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { orderApi } from '../api/menuApi'
import toast from 'react-hot-toast'

const STATUS_COLORS = {
  PENDING:   'badge-neutral',
  CONFIRMED: 'badge-info',
  PREPARING: 'badge-warning',
  READY:     'badge-success',
  DELIVERED: 'badge-success',
  CANCELLED: 'badge-danger',
}

export default function OrderHistory() {
  const { user, isRestaurantOwner, isCustomer } = useAuth()

  const { data: ordersData, isLoading } = useQuery({
    queryKey: ['orders', user?.userId, user?.restaurantId, isRestaurantOwner],
    queryFn: () => isRestaurantOwner
      ? orderApi.getByRestaurant(user.restaurantId)
      : orderApi.getByCustomer(user.userId),
    enabled: !!user,
  })

  const orders = ordersData?.content ?? []

  return (
    <div className="container">
      <div className="page-hero" style={{ paddingBottom: '1rem' }}>
        <h1 className="page-hero__title">{isRestaurantOwner ? 'Restaurant Orders' : 'My Orders'}</h1>
        <p className="page-hero__subtitle">Track all your {isRestaurantOwner ? "restaurant's" : ''} orders</p>
      </div>

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '3rem' }}><div className="loading-spinner" /></div>
      ) : orders.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon">📋</div>
          <div className="empty-state__title">No orders yet</div>
          <p>{isCustomer ? 'Browse the menu and place your first order!' : 'Orders will appear here when customers place them'}</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {orders.map(order => (
            <div key={order.id} className="card">
              <div className="flex justify-between items-center mb-md">
                <div>
                  <span style={{ fontWeight: '700', fontSize: '1.0625rem' }}>Order #{order.id}</span>
                  <span className="text-muted" style={{ fontSize: '0.875rem', marginLeft: '0.75rem' }}>
                    {new Date(order.createdAt).toLocaleString()}
                  </span>
                </div>
                <div className="flex items-center gap-md">
                  <span className={`badge ${STATUS_COLORS[order.status] || 'badge-neutral'}`}>
                    {order.status}
                  </span>
                  <span style={{ fontWeight: '700', color: 'var(--color-primary-light)' }}>
                    ${order.totalAmount.toFixed(2)}
                  </span>
                </div>
              </div>

              {order.items?.map(item => (
                <div key={item.id} className="flex justify-between"
                  style={{ padding: '0.375rem 0', borderTop: '1px solid var(--border-subtle)', fontSize: '0.9rem' }}>
                  <span>{item.menuItemName} × {item.quantity}</span>
                  <span className="text-secondary">${item.subtotal.toFixed(2)}</span>
                </div>
              ))}

              {order.notes && (
                <p className="text-muted" style={{ marginTop: '0.5rem', fontSize: '0.875rem' }}>
                  Note: {order.notes}
                </p>
              )}

              {/* Restaurant owner status update */}
              {isRestaurantOwner && !['DELIVERED', 'CANCELLED'].includes(order.status) && (
                <div className="flex gap-sm mt-md">
                  {order.status === 'PENDING' && (
                    <button className="btn btn-primary btn-sm"
                      id={`confirm-order-${order.id}`}
                      onClick={() => orderApi.updateStatus(order.id, 'CONFIRMED').then(() => toast.success('Order confirmed'))}>
                      Confirm
                    </button>
                  )}
                  {order.status === 'CONFIRMED' && (
                    <button className="btn btn-primary btn-sm"
                      id={`preparing-order-${order.id}`}
                      onClick={() => orderApi.updateStatus(order.id, 'PREPARING').then(() => toast.success('Order in preparation'))}>
                      Start Preparing
                    </button>
                  )}
                  {order.status === 'PREPARING' && (
                    <button className="btn btn-primary btn-sm"
                      id={`ready-order-${order.id}`}
                      onClick={() => orderApi.updateStatus(order.id, 'READY').then(() => toast.success('Order ready!'))}>
                      Mark Ready
                    </button>
                  )}
                  {order.status === 'READY' && (
                    <button className="btn btn-primary btn-sm"
                      id={`delivered-order-${order.id}`}
                      onClick={() => orderApi.updateStatus(order.id, 'DELIVERED').then(() => toast.success('Order delivered!'))}>
                      Mark Delivered
                    </button>
                  )}
                  <button className="btn btn-danger btn-sm"
                    id={`cancel-order-${order.id}`}
                    onClick={() => { if (confirm('Cancel this order?')) orderApi.updateStatus(order.id, 'CANCELLED').then(() => toast.success('Order cancelled')) }}>
                    Cancel
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
