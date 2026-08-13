import { Routes, Route, Link, NavLink } from 'react-router-dom'
import PlacesPage from './pages/PlacesPage'
import TripsNewPage from './pages/TripsNewPage'
import TripDetailPage from './pages/TripDetailPage'
import AuthCallbackPage from './pages/AuthCallbackPage'
import { useAuth } from './contexts/AuthContext'
import { kakaoLoginUrl } from './api/auth'
import './App.css'

function App() {
  const { user, loading, logout } = useAuth()

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/" className="brand">
          travelroute
        </Link>
        <nav>
          <NavLink to="/places">장소</NavLink>
          <NavLink to="/trips/new">여행 만들기</NavLink>
          {!loading &&
            (user ? (
              <span className="auth-status">
                {user.nickname ?? '사용자'}님
                <button className="secondary" onClick={logout}>
                  로그아웃
                </button>
              </span>
            ) : (
              <a className="kakao-login-button" href={kakaoLoginUrl}>
                카카오 로그인
              </a>
            ))}
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<PlacesPage />} />
          <Route path="/places" element={<PlacesPage />} />
          <Route path="/trips/new" element={<TripsNewPage />} />
          <Route path="/trips/:tripId" element={<TripDetailPage />} />
          <Route path="/auth/callback" element={<AuthCallbackPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
