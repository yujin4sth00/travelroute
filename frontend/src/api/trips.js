import { api } from './client'

export const createTrip = (payload) => api.post('/api/trips', payload)

export const getTrip = (tripId) => api.get(`/api/trips/${tripId}`)

export const updateTripDay = (tripId, dayId, payload) =>
  api.patch(`/api/trips/${tripId}/days/${dayId}`, payload)

export const addPlaceToDay = (tripId, dayId, placeId) =>
  api.post(`/api/trips/${tripId}/days/${dayId}/places`, { placeId })

export const removePlaceFromDay = (tripId, dayId, tripDayPlaceId) =>
  api.delete(`/api/trips/${tripId}/days/${dayId}/places/${tripDayPlaceId}`)

export const reorderPlaces = (tripId, dayId, tripDayPlaceIds) =>
  api.patch(`/api/trips/${tripId}/days/${dayId}/places/reorder`, { tripDayPlaceIds })

export const optimizeDay = (tripId, dayId) =>
  api.post(`/api/trips/${tripId}/days/${dayId}/optimize`)

export const getDayRoute = (tripId, dayId) => api.get(`/api/trips/${tripId}/days/${dayId}/route`)

export const autoAssign = (tripId) => api.post(`/api/trips/${tripId}/auto-assign`)
