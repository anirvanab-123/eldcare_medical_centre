import axios from 'axios'

const api = axios.create({
    baseURL: '/api',
    timeout: 10000
})

api.interceptors.request.use(c => {
    const t = sessionStorage.getItem('token')
    if (t) c.headers.Authorization = `Bearer ${t}`
    return c
})

api.interceptors.response.use(
    r => {
        if (r.data.code !== 0) {
            return Promise.reject(new Error(r.data.message))
        }
        return r.data.data
    },
    e => {
        if (e.response?.status === 401) {
            sessionStorage.clear()
            location.href = '/login'
        }
        return Promise.reject(e)
    }
)

export default api
