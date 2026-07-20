import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// ---- Auth ----
export const registerUser = (data) => api.post('/auth/register', data)
export const loginUser = (data) => api.post('/auth/login', data)
export const logoutUser = () => api.post('/auth/logout')
export const resetPassword = (data) => api.post('/auth/reset-password', data)
export const updateProfile = (data) => api.put('/auth/profile', data)
export const getMe = () => api.get('/auth/me')

// ---- Projects / Code submission ----
export const uploadFile = (file, teamId) => {
  const formData = new FormData()
  formData.append('file', file)
  if (teamId) formData.append('teamId', teamId)
  return api.post('/projects/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
export const submitSnippet = (data) => api.post('/projects/snippet', data)
export const listProjects = (search) => api.get('/projects', { params: { search } })
export const listTeamProjects = (teamId, search) => api.get(`/projects/team/${teamId}`, { params: { search } })
export const deleteProject = (projectId) => api.delete(`/projects/${projectId}`)
export const listProjectFiles = (projectId) => api.get(`/projects/${projectId}/files`)
export const refactorFile = (projectId, fileName) =>
  api.post(`/projects/${projectId}/refactor`, null, { params: { fileName } })

// ---- Reviews ----
export const getReview = (reviewId) => api.get(`/reviews/${reviewId}`)
export const getReviewsForProject = (projectId) => api.get(`/reviews/project/${projectId}`)
export const getDocumentation = (projectId) => api.get(`/reviews/project/${projectId}/documentation`)
export const getApiDocs = (projectId) => api.get(`/reviews/project/${projectId}/api-docs`)
export const getReadmeSummary = (projectId) => api.get(`/reviews/project/${projectId}/readme-summary`)

const API_BASE = import.meta.env.VITE_API_URL || "/api";

export const exportReportUrl = (reviewId, format) =>
    `${API_BASE}/reviews/${reviewId}/export/${format}`;
// ---- Teams (Team Workspaces) ----
export const createTeam = (name) => api.post('/teams', { name })
export const listMyTeams = () => api.get('/teams')
export const listTeamMembers = (teamId) => api.get(`/teams/${teamId}/members`)
export const addTeamMember = (teamId, email) => api.post(`/teams/${teamId}/members`, { email })
export const removeTeamMember = (teamId, userId) => api.delete(`/teams/${teamId}/members/${userId}`)

// ---- Analytics (Repository Analytics Dashboard) ----
export const getAnalyticsOverview = () => api.get('/analytics/overview')

// ---- Admin ----
export const adminListUsers = () => api.get('/admin/users')
export const adminDeleteUser = (userId) => api.delete(`/admin/users/${userId}`)
export const adminListProjects = () => api.get('/admin/projects')
export const adminDeleteProject = (projectId) => api.delete(`/admin/projects/${projectId}`)
export const adminStats = () => api.get('/admin/stats')

export default api
