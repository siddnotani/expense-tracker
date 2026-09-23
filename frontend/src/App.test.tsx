import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App routing', () => {
  it('renders the home page and highlights the active nav link', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'Welcome' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Home' })).toHaveClass('nav-link--active')
    expect(screen.getByRole('link', { name: 'Analytics' })).not.toHaveClass('nav-link--active')
  })
})
