import { useEffect, useRef } from 'react'
import { useKakaoMapsSdk } from '../hooks/useKakaoMapsSdk'

export default function KakaoMap({ markers = [], path = [], height = 420 }) {
  const containerRef = useRef(null)
  const { kakao, error, loading } = useKakaoMapsSdk()

  useEffect(() => {
    if (!kakao || !containerRef.current) return

    const center = markers[0]
      ? new kakao.maps.LatLng(markers[0].lat, markers[0].lng)
      : new kakao.maps.LatLng(37.5665, 126.978)

    const map = new kakao.maps.Map(containerRef.current, { center, level: 5 })
    const bounds = new kakao.maps.LatLngBounds()
    const overlays = []

    markers.forEach((marker) => {
      const position = new kakao.maps.LatLng(marker.lat, marker.lng)
      bounds.extend(position)

      const content = document.createElement('div')
      content.className = 'map-marker'
      content.textContent = marker.label ?? ''

      overlays.push(
        new kakao.maps.CustomOverlay({
          map,
          position,
          content,
          yAnchor: 1,
        }),
      )
    })

    let polyline = null
    if (path.length > 1) {
      const linePath = path.map((point) => new kakao.maps.LatLng(point.lat, point.lng))
      linePath.forEach((position) => bounds.extend(position))
      polyline = new kakao.maps.Polyline({
        map,
        path: linePath,
        strokeWeight: 4,
        strokeColor: '#7c3aed',
        strokeOpacity: 0.8,
        strokeStyle: 'solid',
      })
    }

    if (markers.length > 0) {
      map.setBounds(bounds)
    }

    return () => {
      overlays.forEach((overlay) => overlay.setMap(null))
      polyline?.setMap(null)
    }
  }, [kakao, markers, path])

  if (error) {
    return <div className="map-placeholder map-error">{error.message}</div>
  }
  if (loading) {
    return <div className="map-placeholder">지도를 불러오는 중...</div>
  }

  return <div ref={containerRef} style={{ width: '100%', height }} />
}
