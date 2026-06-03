import axios from 'axios'

/**
 * Base Axios instance.
 *
 * SECURITY:
 *  - Base URLs from environment variables — no hardcoded service URLs
 *  - Authorization header injected from AuthContext (token in memory)
 *  - Responses log errors generically — no sensitive data in console
 */

// Auth service instance
export const authAxios = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10_000,
})

// Catalog/Order service instance
export const catalogAxios = axios.create({
  baseURL: import.meta.env.VITE_CATALOG_API_URL || 'http://localhost:8082',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10_000,
})

// Inventory service instance
export const inventoryAxios = axios.create({
  baseURL: import.meta.env.VITE_INVENTORY_API_URL || 'http://localhost:8083',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10_000,
})

/**
 * Injects the Authorization header from the in-memory token.
 * Called by App.jsx when the auth state changes.
 */
export function setAuthToken(token) {
  const header = token ? `Bearer ${token}` : null

  ;[authAxios, catalogAxios, inventoryAxios].forEach(instance => {
    if (header) {
      instance.defaults.headers.common['Authorization'] = header
    } else {
      delete instance.defaults.headers.common['Authorization']
    }
  })
}

// Response interceptor — generic error handling, no sensitive data logged
const addResponseInterceptor = (instance) => {
  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      // SECURITY: do not log error.config (may contain auth headers)
      const status  = error.response?.status
      const message = error.response?.data?.message || error.message
      if (status !== 401 && status !== 403) {
        console.error('API error:', status, message)
      }
      return Promise.reject(error)
    }
  )
}

addResponseInterceptor(authAxios)
addResponseInterceptor(catalogAxios)
addResponseInterceptor(inventoryAxios)
