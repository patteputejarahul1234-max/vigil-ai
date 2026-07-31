import React, { createContext, useContext, useEffect, useState } from 'react'
import { authApi, userApi, UserProfile } from '../api/client'

interface AuthContextValue {
  user: UserProfile | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  refreshUser: () => Promise<void>
  setTokens: (accessToken: string, refreshToken: string) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)

  const refreshUser = async () => {
    const token = localStorage.getItem('vigilai_access_token')
    if (!token) {
      setUser(null)
      setLoading(false)
      return
    }
    try {
      const res = await userApi.me()
      setUser(res.data)
    } catch {
      localStorage.removeItem('vigilai_access_token')
      localStorage.removeItem('vigilai_refresh_token')
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refreshUser()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const setTokens = (accessToken: string, refreshToken: string) => {
    localStorage.setItem('vigilai_access_token', accessToken)
    localStorage.setItem('vigilai_refresh_token', refreshToken)
  }

  const login = async (email: string, password: string) => {
    const res = await authApi.login(email, password)
    setTokens(res.data.accessToken, res.data.refreshToken)
    setUser(res.data.user)
  }

  const logout = () => {
    localStorage.removeItem('vigilai_access_token')
    localStorage.removeItem('vigilai_refresh_token')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refreshUser, setTokens }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
