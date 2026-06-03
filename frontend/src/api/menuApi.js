import { catalogAxios } from './axiosInstance'

export const menuApi = {
  getAll:  (params)    => catalogAxios.get('/menu/items', { params }).then(r => r.data),
  getById: (id)        => catalogAxios.get(`/menu/items/${id}`).then(r => r.data),
  create:  (data)      => catalogAxios.post('/menu/items', data).then(r => r.data),
  update:  (id, data)  => catalogAxios.put(`/menu/items/${id}`, data).then(r => r.data),
  remove:  (id)        => catalogAxios.delete(`/menu/items/${id}`).then(r => r.data),
}

export const orderApi = {
  create:          (data)           => catalogAxios.post('/orders', data).then(r => r.data),
  getById:         (id)             => catalogAxios.get(`/orders/${id}`).then(r => r.data),
  updateStatus:    (id, status)     => catalogAxios.patch(`/orders/${id}/status`, { status }).then(r => r.data),
  getByRestaurant: (restaurantId, params) =>
    catalogAxios.get(`/orders/restaurant/${restaurantId}`, { params }).then(r => r.data),
  getByCustomer:   (customerId, params) =>
    catalogAxios.get(`/orders/customer/${customerId}`, { params }).then(r => r.data),
}
