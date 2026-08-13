import { api } from './client'

export const searchPlaces = (query) => api.get(`/api/places/search?query=${encodeURIComponent(query)}`)

export const listPlaces = () => api.get('/api/places')

export const createPlace = (payload) => api.post('/api/places', payload)

export const deletePlace = (id) => api.delete(`/api/places/${id}`)
