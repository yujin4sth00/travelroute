import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  getTrip,
  updateTripDay,
  addPlaceToDay,
  removePlaceFromDay,
  reorderPlaces,
  optimizeDay,
  getDayRoute,
} from '../api/trips'
import { listPlaces } from '../api/places'
import DayPlaceList from '../components/DayPlaceList'
import KakaoMap from '../components/KakaoMap'

export default function TripDetailPage() {
  const { tripId } = useParams()
  const [trip, setTrip] = useState(null)
  const [selectedDayId, setSelectedDayId] = useState(null)
  const [savedPlaces, setSavedPlaces] = useState([])
  const [selectedPlaceToAdd, setSelectedPlaceToAdd] = useState('')
  const [routeData, setRouteData] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const loadTrip = useCallback(async () => {
    const data = await getTrip(tripId)
    setTrip(data)
    setSelectedDayId((current) => current ?? data.days[0]?.id ?? null)
  }, [tripId])

  useEffect(() => {
    loadTrip().catch((e) => setError(e.message))
    listPlaces()
      .then(setSavedPlaces)
      .catch((e) => setError(e.message))
  }, [loadTrip])

  const selectedDay = useMemo(
    () => trip?.days.find((d) => d.id === selectedDayId) ?? null,
    [trip, selectedDayId],
  )

  useEffect(() => {
    setRouteData(null) // 날짜를 바꾸면 이전에 확인한 경로 결과는 초기화
  }, [selectedDayId])

  const withBusy = async (fn) => {
    setBusy(true)
    setError(null)
    try {
      await fn()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const handleAssignPlace = (field) => (e) => {
    const value = e.target.value ? Number(e.target.value) : null
    withBusy(async () => {
      await updateTripDay(tripId, selectedDayId, {
        startPlaceId: field === 'start' ? value : selectedDay.startPlaceId,
        endPlaceId: field === 'end' ? value : selectedDay.endPlaceId,
      })
      await loadTrip()
    })
  }

  const handleAddPlace = () => {
    if (!selectedPlaceToAdd) return
    withBusy(async () => {
      await addPlaceToDay(tripId, selectedDayId, Number(selectedPlaceToAdd))
      setSelectedPlaceToAdd('')
      await loadTrip()
    })
  }

  const handleRemovePlace = (tripDayPlaceId) =>
    withBusy(async () => {
      await removePlaceFromDay(tripId, selectedDayId, tripDayPlaceId)
      await loadTrip()
    })

  const handleReorder = (orderedIds) =>
    withBusy(async () => {
      await reorderPlaces(tripId, selectedDayId, orderedIds)
      await loadTrip()
    })

  const handleOptimize = () =>
    withBusy(async () => {
      await optimizeDay(tripId, selectedDayId)
      await loadTrip()
    })

  const handleCheckRoute = () =>
    withBusy(async () => {
      setRouteData(await getDayRoute(tripId, selectedDayId))
    })

  if (!trip) {
    return <div className="page">{error ? <p className="error-text">{error}</p> : '불러오는 중...'}</div>
  }

  const placeById = new Map(savedPlaces.map((p) => [p.id, p]))
  const placeIdsInDay = new Set((selectedDay?.places ?? []).map((p) => p.placeId))
  const addablePlaces = savedPlaces.filter((p) => !placeIdsInDay.has(p.id))

  const markers = []
  if (selectedDay?.startPlaceId && placeById.has(selectedDay.startPlaceId)) {
    const p = placeById.get(selectedDay.startPlaceId)
    markers.push({ id: `start-${p.id}`, lat: p.lat, lng: p.lng, label: 'S' })
  }
  selectedDay?.places.forEach((p) => {
    markers.push({ id: `place-${p.id}`, lat: p.lat, lng: p.lng, label: String(p.visitOrder) })
  })
  if (selectedDay?.endPlaceId && placeById.has(selectedDay.endPlaceId)) {
    const p = placeById.get(selectedDay.endPlaceId)
    markers.push({ id: `end-${p.id}`, lat: p.lat, lng: p.lng, label: 'E' })
  }

  const path = routeData
    ? routeData.segments.flatMap((segment) => {
        try {
          return JSON.parse(segment.pathJson)
        } catch {
          return []
        }
      })
    : []

  return (
    <div className="page">
      <div>
        <h1>{trip.title}</h1>
        <p className="muted">
          {trip.startDate} ~ {trip.endDate}
        </p>
      </div>

      <div className="day-tabs">
        {trip.days.map((day) => (
          <button
            key={day.id}
            className={day.id === selectedDayId ? 'day-tab active' : 'day-tab'}
            onClick={() => setSelectedDayId(day.id)}
          >
            Day {day.dayNumber}
          </button>
        ))}
      </div>

      {error && <p className="error-text">{error}</p>}

      {selectedDay && (
        <div className="day-layout">
          <div className="day-panel">
            <div className="place-selectors">
              <label>
                출발지
                <select value={selectedDay.startPlaceId ?? ''} onChange={handleAssignPlace('start')}>
                  <option value="">선택 안 함</option>
                  {savedPlaces.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                도착지
                <select value={selectedDay.endPlaceId ?? ''} onChange={handleAssignPlace('end')}>
                  <option value="">선택 안 함</option>
                  {savedPlaces.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="add-place-row">
              <select value={selectedPlaceToAdd} onChange={(e) => setSelectedPlaceToAdd(e.target.value)}>
                <option value="">추가할 장소 선택</option>
                {addablePlaces.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
              <button onClick={handleAddPlace} disabled={!selectedPlaceToAdd || busy}>
                장소 추가
              </button>
            </div>

            <DayPlaceList places={selectedDay.places} onReorder={handleReorder} onRemove={handleRemovePlace} />

            <div className="action-row">
              <button onClick={handleOptimize} disabled={busy}>
                자동 정렬
              </button>
              <button onClick={handleCheckRoute} disabled={busy}>
                경로 확인
              </button>
            </div>

            {routeData && (
              <p className="muted">
                총 거리 {(routeData.totalDistanceM / 1000).toFixed(1)}km · 총 소요시간 약{' '}
                {Math.round(routeData.totalDurationSec / 60)}분
              </p>
            )}
          </div>

          <div className="day-map">
            <KakaoMap markers={markers} path={path} />
          </div>
        </div>
      )}
    </div>
  )
}
