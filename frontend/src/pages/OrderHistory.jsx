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

const STATUS_LABELS = {
  PENDING:   'Pending',
  CONFIRMED: 'Confirmed',
  PREPARING: 'Preparing',
  READY:     'Ready for Pickup',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
}

export default function OrderHistory() {
  const { user, isRestaurantOwner, isCustomer } = useAuth()
  const queryClient = useQueryClient()

  const ordersQueryKey = ['orders', user?.userId, user?.restaurantId, isRestaurantOwner]

  const { data: ordersData, isLoading } = useQuery({
    queryKey: ordersQueryKey,
    queryFn: () => isRestaurantOwner
      ? orderApi.getByRestaurant(user.restaurantId)
      : orderApi.getByCustomer(user.userId),
    enabled: !!user && (isRestaurantOwner ? !!user.restaurantId : !!user.userId),
    refetchInterval: isRestaurantOwner ? 30_000 : false,
  })

  const updateStatusMutation = useMutation({
    mutationFn: ({ orderId, status }) => orderApi.updateStatus(orderId, status),
    onSuccess: (_, { status }) => {
      queryClient.invalidateQueries({ queryKey: ordersQueryKey })
      const messages = {
        CONFIRMED: 'Order confirmed!',
        PREPARING: 'Order is being prepared',
        READY:     'Order is ready for pickup!',
        DELIVERED: 'Order marked as delivered',
        CANCELLED: 'Order cancelled',
      }
      toast.success(messages[status] || 'Order updated')
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Failed to update order'),
  })

  const orders = ordersData?.content ?? []

  const handleStatus = (orderId, status) => {
    if (status === 'CANCELLED' && !confirm('Cancel this order?')) return
    updateStatusMutation.mutate({ orderId, status })
  }

  return (
    <div className="container">
      <div className="page-hero" style={{ paddingBottom: '1rem' }}>
        <h1 className="page-hero__title">
          {isRestaurantOwner ? 'Incoming Orders' : 'My Orders'}
        </h1>
        <p className="page-hero__subtitle">
          {isRestaurantOwner
            ? 'Manage and update the status of customer orders'
            : 'Track all your orders'}
        </p>
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
                  {isRestaurantOwner && (
                    <span className="text-muted" style={{ fontSize: '0.875rem', marginLeft: '0.75rem' }}>
                      Customer ID: {order.customerId}
                    </span>
                  )}
                  <span className="text-muted" style={{ fontSize: '0.875rem', marginLeft: '0.75rem' }}>
                    {new Date(order.createdAt).toLocaleString()}
                  </span>
                </div>
                <div className="flex items-center gap-md">
                  <span className={`badge ${STATUS_COLORS[order.status] || 'badge-neutral'}`}>
                    {STATUS_LABELS[order.status] || order.status}
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

              {isRestaurantOwner && !['DELIVERED', 'CANCELLED'].includes(order.status) && (
                <div className="flex gap-sm mt-md" style={{ flexWrap: 'wrap' }}>
                  {order.status === 'PENDING' && (
                    <button className="btn btn-primary btn-sm"
                      id={`confirm-order-${order.id}`}
                      disabled={updateStatusMutation.isPending}
                      onClick={() => handleStatus(order.id, 'CONFIRMED')}>
                      ✓ Confirm
                    </button>
                  )}
                  {order.status === 'CONFIRMED' && (
                    <button className="btn btn-primary btn-sm"
                      id={`preparing-order-${order.id}`}
                      disabled={updateStatusMutation.isPending}
                      onClick={() => handleStatus(order.id, 'PREPARING')}>
                      🍳 Start Preparing
                    </button>
                  )}
                  {order.status === 'PREPARING' && (
                    <button className="btn btn-primary btn-sm"
                      id={`ready-order-${order.id}`}
                      disabled={updateStatusMutation.isPending}
                      onClick={() => handleStatus(order.id, 'READY')}>
                      🔔 Mark Ready
                    </button>
                  )}
                  {order.status === 'READY' && (
                    <button className="btn btn-primary btn-sm"
                      id={`delivered-order-${order.id}`}
                      disabled={updateStatusMutation.isPending}
                      onClick={() => handleStatus(order.id, 'DELIVERED')}>
                      ✅ Mark Delivered
                    </button>
                  )}
                  <button className="btn btn-danger btn-sm"
                    id={`cancel-order-${order.id}`}
                    disabled={updateStatusMutation.isPending}
                    onClick={() => handleStatus(order.id, 'CANCELLED')}>
                    ✕ Cancel
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
