import { useEffect, useState } from 'react'

const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY

let loaderPromise = null

function loadKakaoMapsSdk() {
  if (window.kakao?.maps) {
    return Promise.resolve(window.kakao)
  }
  if (loaderPromise) {
    return loaderPromise
  }

  loaderPromise = new Promise((resolve, reject) => {
    if (!KAKAO_JS_KEY) {
      reject(new Error('VITE_KAKAO_JS_KEY가 설정되지 않았습니다. frontend/.env.local을 확인하세요.'))
      return
    }

    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false`
    script.async = true
    script.onerror = () => reject(new Error('Kakao Map SDK 로드에 실패했습니다.'))
    script.onload = () => {
      window.kakao.maps.load(() => resolve(window.kakao))
    }
    document.head.appendChild(script)
  })

  return loaderPromise
}

export function useKakaoMapsSdk() {
  const [state, setState] = useState({ kakao: null, error: null, loading: true })

  useEffect(() => {
    let cancelled = false

    loadKakaoMapsSdk()
      .then((kakao) => {
        if (!cancelled) setState({ kakao, error: null, loading: false })
      })
      .catch((error) => {
        if (!cancelled) setState({ kakao: null, error, loading: false })
      })

    return () => {
      cancelled = true
    }
  }, [])

  return state
}
