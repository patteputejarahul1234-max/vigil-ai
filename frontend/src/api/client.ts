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

// ---------- Stage 2 types ----------

export interface Workspace {
  id: number
  name: string
  description?: string
  ownerId: number
  memberCount: number
  createdAt: string
}

export interface WorkspaceMemberDto {
  userId: number
  fullName: string
  email: string
  avatarUrl?: string
  role: 'OWNER' | 'ADMIN' | 'MEMBER'
}

export interface Project {
  id: number
  workspaceId: number
  name: string
  description?: string
  status: 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'
  createdBy: number
  taskCount: number
  completedTaskCount: number
  createdAt: string
  updatedAt: string
}

export interface Task {
  id: number
  projectId: number
  title: string
  description?: string
  status: 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  assigneeId?: number
  createdBy: number
  dueDate?: string
  attachmentCount: number
  createdAt: string
  updatedAt: string
}

export interface TaskAttachment {
  id: number
  taskId: number
  fileName: string
  downloadUrl: string
  contentType?: string
  sizeBytes: number
  uploadedBy: number
  uploadedAt: string
}

export interface AppNotificationDto {
  id: number
  type: string
  message: string
  relatedEntityId?: number
  relatedEntityType?: string
  read: boolean
  createdAt: string
}

export interface ActivityLogEntry {
  id: number
  actorId: number
  actorName: string
  action: string
  entityType: string
  entityId: number
  details?: string
  createdAt: string
}

export interface AnalyticsSummary {
  totalProjects: number
  totalTasks: number
  completedTasks: number
  completionRate: number
  tasksByStatus: Record<string, number>
  tasksByPriority: Record<string, number>
  overdueTasks: number
}

// ---------- Stage 2 API ----------

export const workspaceApi = {
  list: () => api.get<Workspace[]>('/api/workspaces'),
  create: (name: string, description?: string) =>
    api.post<Workspace>('/api/workspaces', { name, description }),
  get: (id: number) => api.get<Workspace>(`/api/workspaces/${id}`),
  members: (id: number) => api.get<WorkspaceMemberDto[]>(`/api/workspaces/${id}/members`),
  invite: (id: number, email: string, role?: 'ADMIN' | 'MEMBER') =>
    api.post<WorkspaceMemberDto>(`/api/workspaces/${id}/members`, { email, role }),
}

export const projectApi = {
  listForWorkspace: (workspaceId: number) =>
    api.get<Project[]>(`/api/workspaces/${workspaceId}/projects`),
  create: (workspaceId: number, name: string, description?: string) =>
    api.post<Project>(`/api/workspaces/${workspaceId}/projects`, { name, description }),
  get: (projectId: number) => api.get<Project>(`/api/projects/${projectId}`),
  update: (projectId: number, data: Partial<Project>) =>
    api.put<Project>(`/api/projects/${projectId}`, data),
  remove: (projectId: number) => api.delete(`/api/projects/${projectId}`),
}

export const taskApi = {
  listForProject: (projectId: number) => api.get<Task[]>(`/api/projects/${projectId}/tasks`),
  create: (projectId: number, data: Partial<Task>) =>
    api.post<Task>(`/api/projects/${projectId}/tasks`, data),
  get: (taskId: number) => api.get<Task>(`/api/tasks/${taskId}`),
  update: (taskId: number, data: Partial<Task>) => api.put<Task>(`/api/tasks/${taskId}`, data),
  remove: (taskId: number) => api.delete(`/api/tasks/${taskId}`),
  search: (params: Record<string, string | number | undefined>) =>
    api.get<Task[]>('/api/tasks/search', { params }),
}

export const fileApi = {
  upload: (taskId: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post<TaskAttachment>(`/api/tasks/${taskId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  listForTask: (taskId: number) => api.get<TaskAttachment[]>(`/api/tasks/${taskId}/attachments`),
  downloadUrl: (attachmentId: number) => `${API_BASE_URL}/api/files/${attachmentId}/download`,
}

// ---------- Stage 4: Proof of Execution ----------

export interface ProofSubmissionResult {
  id: number
  taskId: number
  verified: boolean
  aiReason: string
  taskStatus: string
  submittedAt: string
}

export const proofApi = {
  submit: (taskId: number, photo: File) => {
    const formData = new FormData()
    formData.append('photo', photo)
    return api.post<ProofSubmissionResult>(`/api/tasks/${taskId}/proof`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

export const notificationApi = {
  list: (unreadOnly = false) =>
    api.get<AppNotificationDto[]>('/api/notifications', { params: { unreadOnly } }),
  unreadCount: () => api.get<{ count: number }>('/api/notifications/unread-count'),
  markRead: (id: number) => api.put(`/api/notifications/${id}/read`),
  markAllRead: () => api.put('/api/notifications/read-all'),
}

export const activityApi = {
  listForWorkspace: (workspaceId: number) =>
    api.get<ActivityLogEntry[]>(`/api/workspaces/${workspaceId}/activity`),
}

export const analyticsApi = {
  getForWorkspace: (workspaceId: number) =>
    api.get<AnalyticsSummary>(`/api/workspaces/${workspaceId}/analytics`),
}
