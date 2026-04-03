import api from './api'

export const sipService = {
  async getDashboard() {
    const res = await api.get('/dashboard')
    return res.data
  },

  async getAllSips() {
    const res = await api.get('/sip/all')
    return res.data
  },

  async getSipById(id) {
    const res = await api.get(`/sip/${id}`)
    return res.data
  },

  async createSip(data) {
    const res = await api.post('/sip/create', data)
    return res.data
  },
}
