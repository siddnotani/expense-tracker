import { Link } from 'react-router-dom'

export function HomePage() {
  return (
    <section className="page">
      <h1>Welcome</h1>
      <p>Track your spending and see where your money goes.</p>
      <Link to="/analytics" className="button">
        View analytics
      </Link>
    </section>
  )
}
