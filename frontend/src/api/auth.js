import { api } from './client'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const kakaoLoginUrl = `${API_BASE_URL}/api/auth/kakao/login`

export const getCurrentUser = () => api.get('/api/auth/me')
