import { Routes, Route, Link, NavLink } from 'react-router-dom'
import PlacesPage from './pages/PlacesPage'
import TripsNewPage from './pages/TripsNewPage'
import TripDetailPage from './pages/TripDetailPage'
import './App.css'

function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/" className="brand">
          travelroute
        </Link>
        <nav>
          <NavLink to="/places">장소</NavLink>
          <NavLink to="/trips/new">여행 만들기</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<PlacesPage />} />
          <Route path="/places" element={<PlacesPage />} />
          <Route path="/trips/new" element={<TripsNewPage />} />
          <Route path="/trips/:tripId" element={<TripDetailPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
