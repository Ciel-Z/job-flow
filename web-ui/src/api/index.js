import axios from 'axios'

const api = axios.create({
  baseURL: '',
  timeout: 10000
})

// Response interceptor
api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// Job APIs
export const jobApi = {
  list(params) {
    return api.get('/job/list', { params })
  },
  save(data) {
    return api.post('/job/save', data)
  },
  toggle(jobId) {
    return api.get(`/job/toggle/${jobId}`)
  },
  delete(jobId) {
    return api.get(`/job/delete?/${jobId}`)
  },
  detail(jobId) {
    return api.get(`/job/detail/${jobId}`)
  },
  start(jobId, params) {
    return api.get(`/job/start/${jobId}`, { params })
  },
  stop(data) {
    return api.post('/job/stop', data)
  },
  instanceList(params) {
    return api.get('/job/instance/list', { params })
  },
  log(instanceId) {
    return api.get(`/job/log/${instanceId}`)
  }
}

// Workflow APIs
export const workflowApi = {
  list(params) {
    return api.get('/job/flow/list', { params })
  },
  save(data) {
    return api.post('/job/flow/save', data)
  },
  delete(flowId) {
    return api.get(`/job/flow/delete/${flowId}`)
  },
  detail(flowId) {
    return api.get(`/job/flow/detail/${flowId}`)
  },
  start(flowId) {
    return api.get(`/job/flow/start/${flowId}`)
  },
  stop(instanceId) {
    return api.get(`/job/flow/stop/${instanceId}`)
  },
  retry(instanceId, nodeId) {
    return api.get(`/job/flow/retry/${instanceId}/${nodeId}`)
  },
  instanceList(params) {
    return api.get('/job/flow/instance/list', { params })
  },
  progress(flowInstanceId) {
    return api.get(`/job/flow/progress/${flowInstanceId}`)
  }
}

export default api
