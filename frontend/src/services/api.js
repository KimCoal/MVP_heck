import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

export const uploadCadFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  const response = await api.post('/cad/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  return response.data
}

export const getCadFiles = async () => {
  const response = await api.get('/cad/files')
  return response.data
}

export const getCadFileById = async (id) => {
  const response = await api.get(`/cad/files/${id}`)
  return response.data
}

export const getGlbFileUrl = (id) => {
  return `${API_BASE_URL}/cad/files/${id}/glb`
}

export const getPartById = async (id) => {
  const response = await api.get(`/parts/${id}`)
  return response.data
}

export const savePartNote = async (partId, note) => {
  const response = await api.post(`/parts/${partId}/note`, { note })
  return response.data
}

export const deletePartNote = async (partId) => {
  const response = await api.delete(`/parts/${partId}/note`)
  return response.data
}
