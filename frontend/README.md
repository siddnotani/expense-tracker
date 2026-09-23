# Expense Tracker — Frontend

React 18 + TypeScript SPA built with Vite. Provides the `/analytics` page that visualises
`GET /api/analytics/summary` from the Spring Boot BFF (`bff/`).

## Running locally

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

The dev server proxies `/api/*` to `http://localhost:8080` (see `vite.config.ts`), so start
the BFF first and the Analytics page will load real data.

## Environment variables

Copy `.env.example` to `.env.local` and adjust if needed:

| Variable            | Default | Description                                                                                  |
| ------------------- | ------- | -------------------------------------------------------------------------------------------- |
| `VITE_API_BASE_URL` | `""`    | Base URL of the BFF. Empty means relative requests (`/api/...`) that go through the Vite proxy. |

## Scripts

| Command           | Description                         |
| ----------------- | ----------------------------------- |
| `npm run dev`     | Start the dev server                |
| `npm run build`   | Type-check and build to `dist/`     |
| `npm run lint`    | Run ESLint                          |
| `npm test`        | Run Vitest (non-interactive)        |
| `npm run preview` | Preview the production build        |

## Structure

```
src/
  api/          fetch client + typed analytics endpoint
  components/   NavBar, StatCard, charts (recharts)
  pages/        HomePage, AnalyticsPage
  types/        API response types (shared JSON contract)
  utils/        currency / month formatting
```
