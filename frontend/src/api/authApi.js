import { authAxios } from './axiosInstance'

export const authApi = {
  register: (data) => authAxios.post('/auth/register', data).then(r => r.data),
  login:    (data) => authAxios.post('/auth/login',    data).then(r => r.data),
  logout:   ()     => authAxios.post('/auth/logout').then(r => r.data),
  refresh:  (data) => authAxios.post('/auth/refresh', data).then(r => r.data),
}
