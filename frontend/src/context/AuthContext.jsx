import React, { createContext, useContext, useState, useCallback } from 'react'

/**
 * AuthContext — stores user identity and JWT access token in memory only.
 *
 * SECURITY:
 *  - Token stored in React state (memory), NOT localStorage or sessionStorage
 *  - Cleared on logout and page refresh
 *  - Axios interceptor reads from this context to add Authorization headers
 *  TODO(security): Consider silent token refresh strategy for better UX on refresh
 */

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  // SECURITY: token lives in memory — no persistence across page refresh (by design)
  const [token, setToken] = useState(null)
  const [user, setUser] = useState(null)   // { userId, username, roles, restaurantId }

  const login = useCallback((tokenResponse) => {
    setToken(tokenResponse.accessToken)
    setUser({
      userId:       tokenResponse.userId,
      username:     tokenResponse.username,
      roles:        tokenResponse.roles,
      restaurantId: tokenResponse.restaurantId,
      refreshToken: tokenResponse.refreshToken,
    })
  }, [])

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
    // SECURITY: full page redirect clears any cached state
    window.location.href = '/login'
  }, [])

  const isAuthenticated = !!token
  const isRestaurantOwner = user?.roles?.includes('RESTAURANT_OWNER') ?? false
  const isCustomer = user?.roles?.includes('CUSTOMER') ?? false
  const isAdmin = user?.roles?.includes('ADMIN') ?? false

  return (
    <AuthContext.Provider value={{
      token, user,
      login, logout,
      isAuthenticated, isRestaurantOwner, isCustomer, isAdmin,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
