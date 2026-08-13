import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { setToken } from '../api/tokenStorage'
import { useAuth } from '../contexts/AuthContext'

export default function AuthCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { refresh } = useAuth()

  useEffect(() => {
    const token = searchParams.get('token')
    if (token) {
      setToken(token)
      refresh().then(() => navigate('/', { replace: true }))
    } else {
      navigate('/', { replace: true })
    }
    // 최초 진입 시 한 번만 처리
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return <div className="page">로그인 처리 중...</div>
}
