import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { menuApi, orderApi } from '../api/menuApi'
import { inventoryApi } from '../api/inventoryApi'
import toast from 'react-hot-toast'

const STATUS_COLORS = {
  PENDING:   'badge-neutral',
  CONFIRMED: 'badge-info',
  PREPARING: 'badge-warning',
  READY:     'badge-success',
  DELIVERED: 'badge-success',
  CANCELLED: 'badge-danger',
}

export default function RestaurantDashboard() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const restaurantId = user?.restaurantId

  const [activeTab, setActiveTab] = useState('menu')
  const [showMenuForm, setShowMenuForm] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [menuForm, setMenuForm] = useState({ name: '', description: '', price: '', category: '', imageUrl: '' })

  const [showInventoryForm, setShowInventoryForm] = useState(false)
  const [inventoryForm, setInventoryForm] = useState({ menuItemId: '', name: '', quantity: '', unit: '', threshold: '10', expiryDate: '' })

  // ── Queries ──────────────────────────────────────────────────────────────

  const { data: menuData, isLoading: menuLoading } = useQuery({
    queryKey: ['menu', restaurantId],
    queryFn: () => menuApi.getAll({ restaurantId }),
    enabled: !!restaurantId,
  })

  const { data: inventoryData, isLoading: inventoryLoading } = useQuery({
    queryKey: ['inventory'],
    queryFn: inventoryApi.getItems,
  })

  const { data: surplusData } = useQuery({
    queryKey: ['surplus'],
    queryFn: inventoryApi.getSurplus,
    refetchInterval: 60_000,
  })

  const { data: ordersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['restaurant-orders', restaurantId],
    queryFn: () => orderApi.getByRestaurant(restaurantId),
    enabled: !!restaurantId,
    refetchInterval: 30_000,
  })

  // ── Menu mutations ────────────────────────────────────────────────────────

  const createMenuMutation = useMutation({
    mutationFn: menuApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['menu'] })
      setShowMenuForm(false)
      resetMenuForm()
      toast.success('Menu item created!')
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Failed to create item'),
  })

  const updateMenuMutation = useMutation({
    mutationFn: ({ id, data }) => menuApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['menu'] })
      setEditingItem(null)
      resetMenuForm()
      toast.success('Menu item updated!')
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Failed to update item'),
  })

  const deleteMenuMutation = useMutation({
    mutationFn: menuApi.remove,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['menu'] }); toast.success('Menu item deleted') },
    onError: () => toast.error('Failed to delete item'),
  })

  // ── Inventory mutations ───────────────────────────────────────────────────

  const createInventoryMutation = useMutation({
    mutationFn: inventoryApi.createItem,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      queryClient.invalidateQueries({ queryKey: ['surplus'] })
      setShowInventoryForm(false)
      setInventoryForm({ menuItemId: '', name: '', quantity: '', unit: '', threshold: '10', expiryDate: '' })
      toast.success('Inventory item added!')
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Failed to add inventory'),
  })

  // ── Order status mutation ────────────────────────────────────────────────

  const updateOrderMutation = useMutation({
    mutationFn: ({ orderId, status }) => orderApi.updateStatus(orderId, status),
    onSuccess: (_, { status }) => {
      queryClient.invalidateQueries({ queryKey: ['restaurant-orders', restaurantId] })
      const messages = {
        CONFIRMED: 'Order confirmed!',
        PREPARING: 'Preparing the order',
        READY:     'Order is ready for pickup!',
        DELIVERED: 'Order delivered!',
        CANCELLED: 'Order cancelled',
      }
      toast.success(messages[status] || 'Order updated')
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Failed to update order'),
  })

  // ── Helpers ───────────────────────────────────────────────────────────────

  const resetMenuForm = () => setMenuForm({ name: '', description: '', price: '', category: '', imageUrl: '' })

  const handleMenuSubmit = (e) => {
    e.preventDefault()
    const payload = { ...menuForm, price: parseFloat(menuForm.price) }
    if (editingItem) {
      updateMenuMutation.mutate({ id: editingItem.id, data: payload })
    } else {
      createMenuMutation.mutate(payload)
    }
  }

  const handleEdit = (item) => {
    setEditingItem(item)
    setMenuForm({ name: item.name, description: item.description || '', price: String(item.price), category: item.category || '', imageUrl: item.imageUrl || '' })
    setShowMenuForm(true)
    setActiveTab('menu')
  }

  const handleOrderStatus = (orderId, status) => {
    if (status === 'CANCELLED' && !confirm('Cancel this order?')) return
    updateOrderMutation.mutate({ orderId, status })
  }

  const menuItems      = menuData?.content ?? []
  const inventoryItems = inventoryData ?? []
  const surplusItems   = surplusData ?? []
  const orders         = ordersData?.content ?? []
  const pendingOrders  = orders.filter(o => o.status === 'PENDING').length

  return (
    <div className="container">
      <div className="page-hero" style={{ paddingBottom: '1rem' }}>
        <h1 className="page-hero__title">Restaurant Dashboard</h1>
        <p className="page-hero__subtitle">Manage your menu, inventory, orders, and surplus alerts</p>
      </div>

      {/* Stat Cards */}
      <div className="grid-4 mb-lg">
        {[
          { label: 'Menu Items',    value: menuItems.length,      icon: '🍽️' },
          { label: 'Inventory',     value: inventoryItems.length, icon: '📦' },
          { label: 'Pending Orders',value: pendingOrders,         icon: '🛒' },
          { label: 'Restaurant ID', value: restaurantId ?? '—',   icon: '🏪' },
        ].map(stat => (
          <div key={stat.label} className="card stat-card"
            style={stat.label === 'Pending Orders' && pendingOrders > 0 ? { borderColor: 'var(--color-warning, #f59e0b)' } : {}}>
            <span style={{ fontSize: '1.75rem' }}>{stat.icon}</span>
            <div className="stat-card__label">{stat.label}</div>
            <div className="stat-card__value">{stat.value}</div>
          </div>
        ))}
      </div>

      {/* Tabs */}
      <div className="flex gap-sm mb-lg">
        {['menu', 'orders', 'inventory'].map(tab => (
          <button key={tab} id={`tab-${tab}`}
            className={`btn ${activeTab === tab ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => setActiveTab(tab)}
            style={{ position: 'relative' }}>
            {tab === 'menu' ? '🍽️ Menu' : tab === 'orders' ? '🛒 Orders' : '📦 Inventory'}
            {tab === 'orders' && pendingOrders > 0 && (
              <span style={{
                position: 'absolute', top: '-6px', right: '-6px',
                background: 'var(--color-danger, #ef4444)', color: '#fff',
                borderRadius: '999px', fontSize: '0.7rem', fontWeight: '700',
                minWidth: '18px', height: '18px', display: 'flex',
                alignItems: 'center', justifyContent: 'center', padding: '0 4px',
              }}>{pendingOrders}</span>
            )}
          </button>
        ))}
      </div>

      {/* ── Menu Tab ───────────────────────────────────────────────────────── */}
      {activeTab === 'menu' && (
        <>
          <div className="flex justify-between items-center mb-md">
            <h2 style={{ fontSize: '1.25rem', fontWeight: '700' }}>Menu Items</h2>
            <button id="add-menu-item-btn" className="btn btn-primary"
              onClick={() => { setShowMenuForm(!showMenuForm); setEditingItem(null); resetMenuForm() }}>
              {showMenuForm ? 'Cancel' : '+ Add Item'}
            </button>
          </div>

          {showMenuForm && (
            <div className="card mb-lg">
              <h3 style={{ marginBottom: '1rem', fontWeight: '600' }}>
                {editingItem ? 'Edit Menu Item' : 'New Menu Item'}
              </h3>
              <form onSubmit={handleMenuSubmit} id="menu-item-form">
                <div className="grid-2" style={{ gap: '1rem', marginBottom: '1rem' }}>
                  <div className="form-group">
                    <label className="form-label">Name *</label>
                    <input className="form-control" required value={menuForm.name}
                      onChange={e => setMenuForm(p => ({ ...p, name: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Price ($) *</label>
                    <input className="form-control" type="number" step="0.01" min="0" required
                      value={menuForm.price}
                      onChange={e => setMenuForm(p => ({ ...p, price: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Category</label>
                    <input className="form-control" value={menuForm.category}
                      onChange={e => setMenuForm(p => ({ ...p, category: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Image URL</label>
                    <input className="form-control" type="url" value={menuForm.imageUrl}
                      onChange={e => setMenuForm(p => ({ ...p, imageUrl: e.target.value }))} />
                  </div>
                </div>
                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <label className="form-label">Description</label>
                  <textarea className="form-control" rows={3} value={menuForm.description}
                    onChange={e => setMenuForm(p => ({ ...p, description: e.target.value }))} />
                </div>
                <button type="submit" id="save-menu-item-btn" className="btn btn-primary"
                  disabled={createMenuMutation.isPending || updateMenuMutation.isPending}>
                  {(createMenuMutation.isPending || updateMenuMutation.isPending) ? 'Saving...' : 'Save Item'}
                </button>
              </form>
            </div>
          )}

          {menuLoading ? (
            <div style={{ padding: '2rem', textAlign: 'center' }}><div className="loading-spinner" /></div>
          ) : menuItems.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state__icon">🍽️</div>
              <div className="empty-state__title">No menu items yet</div>
              <p>Add your first menu item to get started</p>
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr><th>Name</th><th>Category</th><th>Price</th><th>Available</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  {menuItems.map(item => (
                    <tr key={item.id}>
                      <td><strong>{item.name}</strong><br /><small className="text-muted">{item.description}</small></td>
                      <td>{item.category || '—'}</td>
                      <td>${item.price.toFixed(2)}</td>
                      <td><span className={`badge ${item.available ? 'badge-success' : 'badge-neutral'}`}>{item.available ? 'Yes' : 'No'}</span></td>
                      <td>
                        <div className="flex gap-sm">
                          <button id={`edit-menu-${item.id}`} className="btn btn-ghost btn-sm" onClick={() => handleEdit(item)}>Edit</button>
                          <button id={`delete-menu-${item.id}`} className="btn btn-danger btn-sm"
                            onClick={() => { if (confirm('Delete this item?')) deleteMenuMutation.mutate(item.id) }}>Delete</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* ── Orders Tab ─────────────────────────────────────────────────────── */}
      {activeTab === 'orders' && (
        <>
          <div className="flex justify-between items-center mb-md">
            <h2 style={{ fontSize: '1.25rem', fontWeight: '700' }}>
              Customer Orders
              {pendingOrders > 0 && (
                <span className="badge badge-warning" style={{ marginLeft: '0.75rem' }}>
                  {pendingOrders} pending
                </span>
              )}
            </h2>
          </div>

          {ordersLoading ? (
            <div style={{ padding: '2rem', textAlign: 'center' }}><div className="loading-spinner" /></div>
          ) : orders.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state__icon">🛒</div>
              <div className="empty-state__title">No orders yet</div>
              <p>Orders from customers will appear here automatically</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {orders.map(order => (
                <div key={order.id} className="card"
                  style={order.status === 'PENDING' ? { borderLeft: '3px solid var(--color-warning, #f59e0b)' } : {}}>
                  <div className="flex justify-between items-center mb-md">
                    <div>
                      <span style={{ fontWeight: '700', fontSize: '1.0625rem' }}>Order #{order.id}</span>
                      <span className="text-muted" style={{ fontSize: '0.875rem', marginLeft: '0.75rem' }}>
                        Customer #{order.customerId}
                      </span>
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

                  {/* Order items */}
                  <div style={{ marginBottom: '0.5rem' }}>
                    {order.items?.map(item => (
                      <div key={item.id} className="flex justify-between"
                        style={{ padding: '0.3rem 0', borderTop: '1px solid var(--border-subtle)', fontSize: '0.9rem' }}>
                        <span>{item.menuItemName} × {item.quantity}</span>
                        <span className="text-secondary">${item.subtotal.toFixed(2)}</span>
                      </div>
                    ))}
                  </div>

                  {order.notes && (
                    <p className="text-muted" style={{ fontSize: '0.875rem', marginBottom: '0.5rem' }}>
                      📝 {order.notes}
                    </p>
                  )}

                  {/* Status action buttons */}
                  {!['DELIVERED', 'CANCELLED'].includes(order.status) && (
                    <div className="flex gap-sm mt-md" style={{ flexWrap: 'wrap' }}>
                      {order.status === 'PENDING' && (
                        <button className="btn btn-primary btn-sm"
                          id={`confirm-order-${order.id}`}
                          disabled={updateOrderMutation.isPending}
                          onClick={() => handleOrderStatus(order.id, 'CONFIRMED')}>
                          ✓ Confirm Order
                        </button>
                      )}
                      {order.status === 'CONFIRMED' && (
                        <button className="btn btn-primary btn-sm"
                          id={`preparing-order-${order.id}`}
                          disabled={updateOrderMutation.isPending}
                          onClick={() => handleOrderStatus(order.id, 'PREPARING')}>
                          🍳 Start Preparing
                        </button>
                      )}
                      {order.status === 'PREPARING' && (
                        <button className="btn btn-primary btn-sm"
                          id={`ready-order-${order.id}`}
                          disabled={updateOrderMutation.isPending}
                          onClick={() => handleOrderStatus(order.id, 'READY')}>
                          🔔 Mark Ready
                        </button>
                      )}
                      {order.status === 'READY' && (
                        <button className="btn btn-primary btn-sm"
                          id={`delivered-order-${order.id}`}
                          disabled={updateOrderMutation.isPending}
                          onClick={() => handleOrderStatus(order.id, 'DELIVERED')}>
                          ✅ Mark Delivered
                        </button>
                      )}
                      <button className="btn btn-danger btn-sm"
                        id={`cancel-order-${order.id}`}
                        disabled={updateOrderMutation.isPending}
                        onClick={() => handleOrderStatus(order.id, 'CANCELLED')}>
                        ✕ Cancel
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* ── Inventory Tab ──────────────────────────────────────────────────── */}
      {activeTab === 'inventory' && (
        <>
          <div className="flex justify-between items-center mb-md">
            <h2 style={{ fontSize: '1.25rem', fontWeight: '700' }}>Inventory</h2>
            <button id="add-inventory-btn" className="btn btn-primary"
              onClick={() => setShowInventoryForm(!showInventoryForm)}>
              {showInventoryForm ? 'Cancel' : '+ Add Item'}
            </button>
          </div>

          {showInventoryForm && (
            <div className="card mb-lg">
              <h3 style={{ marginBottom: '1rem', fontWeight: '600' }}>New Inventory Item</h3>
              <form onSubmit={(e) => {
                e.preventDefault()
                createInventoryMutation.mutate({
                  ...inventoryForm,
                  menuItemId: Number(inventoryForm.menuItemId),
                  quantity: Number(inventoryForm.quantity),
                  threshold: Number(inventoryForm.threshold),
                })
              }} id="inventory-item-form">
                <div className="grid-3" style={{ gap: '1rem', marginBottom: '1rem' }}>
                  <div className="form-group">
                    <label className="form-label">Menu Item ID *</label>
                    <input className="form-control" type="number" required value={inventoryForm.menuItemId}
                      onChange={e => setInventoryForm(p => ({ ...p, menuItemId: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Name *</label>
                    <input className="form-control" required value={inventoryForm.name}
                      onChange={e => setInventoryForm(p => ({ ...p, name: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Quantity *</label>
                    <input className="form-control" type="number" min="0" required value={inventoryForm.quantity}
                      onChange={e => setInventoryForm(p => ({ ...p, quantity: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Unit (e.g. kg, pcs)</label>
                    <input className="form-control" value={inventoryForm.unit}
                      onChange={e => setInventoryForm(p => ({ ...p, unit: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Surplus Threshold</label>
                    <input className="form-control" type="number" min="1" value={inventoryForm.threshold}
                      onChange={e => setInventoryForm(p => ({ ...p, threshold: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Expiry Date</label>
                    <input className="form-control" type="date" value={inventoryForm.expiryDate}
                      onChange={e => setInventoryForm(p => ({ ...p, expiryDate: e.target.value }))} />
                  </div>
                </div>
                <button type="submit" id="save-inventory-btn" className="btn btn-primary"
                  disabled={createInventoryMutation.isPending}>
                  {createInventoryMutation.isPending ? 'Adding...' : 'Add to Inventory'}
                </button>
              </form>
            </div>
          )}

          {inventoryLoading ? (
            <div style={{ padding: '2rem', textAlign: 'center' }}><div className="loading-spinner" /></div>
          ) : inventoryItems.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state__icon">📦</div>
              <div className="empty-state__title">No inventory tracked yet</div>
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr><th>Item</th><th>Qty</th><th>Unit</th><th>Threshold</th><th>Expiry</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {inventoryItems.map(item => (
                    <tr key={item.id}>
                      <td><strong>{item.name}</strong></td>
                      <td>{item.quantity}</td>
                      <td>{item.unit || '—'}</td>
                      <td>{item.threshold}</td>
                      <td>{item.expiryDate || '—'}</td>
                      <td>
                        {item.surplus
                          ? <span className="badge badge-warning surplus-badge">⚠️ Surplus</span>
                          : <span className="badge badge-success">✓ OK</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  )
}
