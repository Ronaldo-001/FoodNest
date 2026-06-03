import { inventoryAxios } from './axiosInstance'

export const inventoryApi = {
  getItems:   ()         => inventoryAxios.get('/inventory/items').then(r => r.data),
  createItem: (data)     => inventoryAxios.post('/inventory/items', data).then(r => r.data),
  updateItem: (id, data) => inventoryAxios.put(`/inventory/items/${id}`, data).then(r => r.data),
  getSurplus: ()         => inventoryAxios.get('/inventory/surplus').then(r => r.data),
}

export const notificationApi = {
  subscribe:    (data) => inventoryAxios.post('/notifications/subscribe', data).then(r => r.data),
  getHistory:   ()     => inventoryAxios.get('/notifications/history').then(r => r.data),
}
