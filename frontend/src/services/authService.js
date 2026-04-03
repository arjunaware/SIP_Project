import api from './api'

export const authService = {
  async signup(data) {
    const res = await api.post('/auth/signup', data)
    return res.data
  },

  async login(data) {
    const res = await api.post('/auth/login', data)
    return res.data
  },
}
