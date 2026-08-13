import { useEffect, useState } from 'react'
import { searchPlaces, listPlaces, createPlace, deletePlace } from '../api/places'

export default function PlacesPage() {
  const [query, setQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [savedPlaces, setSavedPlaces] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const loadSavedPlaces = async () => {
    try {
      setSavedPlaces(await listPlaces())
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => {
    loadSavedPlaces()
  }, [])

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!query.trim()) return

    setLoading(true)
    setError(null)
    try {
      setSearchResults(await searchPlaces(query.trim()))
    } catch (e) {
      setError(e.message)
      setSearchResults([])
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async (result) => {
    setError(null)
    try {
      await createPlace({
        name: result.name,
        address: result.address,
        lat: result.lat,
        lng: result.lng,
        category: result.category,
        kakaoPlaceId: result.kakaoPlaceId,
      })
      await loadSavedPlaces()
    } catch (e) {
      setError(e.message)
    }
  }

  const handleDelete = async (id) => {
    setError(null)
    try {
      await deletePlace(id)
      await loadSavedPlaces()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="page">
      <div>
        <h1>장소 검색</h1>
        <p className="muted">Kakao 지도에서 방문하고 싶은 장소를 검색하고 여행에 저장하세요.</p>
      </div>

      <form onSubmit={handleSearch} className="search-form">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="장소명을 입력하세요 (예: 강남역 카페)"
        />
        <button type="submit" disabled={loading}>
          {loading ? '검색 중...' : '검색'}
        </button>
      </form>

      {error && <p className="error-text">{error}</p>}

      <section>
        <h2>검색 결과</h2>
        {searchResults.length === 0 ? (
          <p className="muted">검색 결과가 없습니다.</p>
        ) : (
          <ul className="place-list">
            {searchResults.map((result) => (
              <li key={result.kakaoPlaceId} className="place-item">
                <div>
                  <strong>{result.name}</strong>
                  <p>{result.address}</p>
                  {result.category && <span className="badge">{result.category}</span>}
                </div>
                <button onClick={() => handleSave(result)}>저장</button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2>저장된 장소</h2>
        {savedPlaces.length === 0 ? (
          <p className="muted">저장된 장소가 없습니다.</p>
        ) : (
          <ul className="place-list">
            {savedPlaces.map((place) => (
              <li key={place.id} className="place-item">
                <div>
                  <strong>{place.name}</strong>
                  <p>{place.address}</p>
                </div>
                <button className="danger" onClick={() => handleDelete(place.id)}>
                  삭제
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
