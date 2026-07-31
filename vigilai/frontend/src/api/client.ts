import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const api = axios.create({
  baseURL: API_BASE_URL,
})

// Attach the access token to every outgoing request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('vigilai_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export interface UserProfile {
  id: number
  fullName: string
  email: string
  role: 'USER' | 'ADMIN'
  emailVerified: boolean
  avatarUrl?: string
  bio?: string
  timezone?: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  user: UserProfile
}

export const authApi = {
  register: (fullName: string, email: string, password: string) =>
    api.post<UserProfile>('/api/auth/register', { fullName, email, password }),

  login: (email: string, password: string) =>
    api.post<AuthResponse>('/api/auth/login', { email, password }),

  verifyEmail: (token: string) =>
    api.get('/api/auth/verify-email', { params: { token } }),

  resendVerification: (email: string) =>
    api.post('/api/auth/resend-verification', null, { params: { email } }),

  forgotPassword: (email: string) =>
    api.post('/api/auth/forgot-password', { email }),

  resetPassword: (token: string, newPassword: string) =>
    api.post('/api/auth/reset-password', { token, newPassword }),
}

export const userApi = {
  me: () => api.get<UserProfile>('/api/users/me'),
  updateProfile: (data: Partial<UserProfile>) => api.put<UserProfile>('/api/users/me', data),
}

export function oauthUrl(provider: 'google' | 'github') {
  return `${API_BASE_URL}/oauth2/authorization/${provider}`
}
