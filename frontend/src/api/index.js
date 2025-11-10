import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080/api/v1'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Response interceptor
apiClient.interceptors.response.use(
  response => {
    const { data } = response
    if (data.code === 0) {
      return data
    } else {
      return Promise.reject(new Error(data.message || 'API Error'))
    }
  },
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// Project APIs
export const projectApi = {
  list: (params) => apiClient.get('/projects', { params }),
  create: (data) => apiClient.post('/projects', data),
  get: (projectId) => apiClient.get(`/projects/${projectId}`),
  update: (data) => apiClient.put('/projects', data),
  delete: (projectId) => apiClient.delete(`/projects/${projectId}`)
}

// Topology APIs
export const topologyApi = {
  save: (projectId, data) => apiClient.post(`/projects/${projectId}/topology`, data)
}

// Simulation APIs
export const simulationApi = {
  start: (projectId) => apiClient.get(`/simulations/start/${projectId}`),
  stop: (projectId) => apiClient.get(`/simulations/stop/${projectId}`)
}

// Results APIs
export const resultApi = {
  query: (params) => apiClient.get('/simulation-results', { params }),
  export: (params) => apiClient.get('/simulation-results/export', { 
    params,
    responseType: 'blob'
  }),
  aggregate: (params) => apiClient.get('/simulation-results/aggregate', { params }),
  getVector: (params) => apiClient.get('/simulation-results/vector', { params })
}

export default apiClient
