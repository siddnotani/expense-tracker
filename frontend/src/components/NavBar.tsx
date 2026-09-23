import { NavLink } from 'react-router-dom'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link nav-link--active' : 'nav-link'

export function NavBar() {
  return (
    <header className="nav">
      <span className="nav-brand">Expense Tracker</span>
      <nav className="nav-links" aria-label="Main navigation">
        <NavLink to="/" end className={linkClass}>
          Home
        </NavLink>
        <NavLink to="/analytics" className={linkClass}>
          Analytics
        </NavLink>
      </nav>
    </header>
  )
}
