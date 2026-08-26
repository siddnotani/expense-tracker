# Expense Tracker

A personal expense tracker built as a React SPA backed by Java 21 Spring Boot microservices.

## Prerequisites

- Java 21
- Maven 3.6+
- Node.js 20+ and npm
- Docker 27+ with Docker Compose

## Run locally

```bash
make dev
```

The Vite development server is available at http://localhost:5173.

## Ports

| Component | Port |
|---|---:|
| BFF | 8080 |
| Auth service | 8081 |
| Accounts service | 8082 |
| Transaction service | 8083 |
| Budget service | 8084 |
| Vite SPA | 5173 |
| Postgres | 5432 |

## Architecture

```
                    ┌───────────────────────────────┐
                    │  React 18 SPA (Vite + TS)     │
                    │  browser                      │
                    └───────────────┬───────────────┘
                                    │ REST/JSON, httpOnly session cookie
                                    v
                    ┌───────────────────────────────┐
                    │  bff  :8080                   │
                    │  Java 21 · Spring Boot 3      │
                    │  Spring Web + WebClient       │  <- the only service the SPA talks to
                    │  session cookie, fan-out      │
                    └───┬────────┬────────┬─────┬───┘
             internal   │        │        │     │   (HTTP + short-lived JWT)
                        v        v        v     v
    ┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐ ┌──────────────────┐
    │ auth-service     │ │ accounts-    │ │ transaction-     │ │ budget-service   │
    │ :8081            │ │ service :8082│ │ service :8083    │ │ :8084            │
    │ Spring Boot 3    │ │ Spring Boot 3│ │ Spring Boot 3    │ │ Spring Boot 3    │
    │ Spring Security  │ │ Spring Data  │ │ Spring Data JPA  │ │ Spring Data JPA  │
    │ BCrypt · JWT     │ │ JPA          │ │                  │ │                  │
    │ users            │ │ accounts,    │ │ expense/income   │ │ categories,      │
    │                  │ │ balances     │ │ ledger           │ │ monthly budgets  │
    └────────┬─────────┘ └──────┬───────┘ └────────┬─────────┘ └────────┬─────────┘
             │ Flyway           │ Flyway           │ Flyway             │ Flyway
             v                  v                  v                    v
    ┌─────────────────────────────────────────────────────────────────────────────┐
    │  Postgres 16 — one instance, one schema per service                         │
    │  auth.*            accounts.*          txn.*              budget.*          │
    └─────────────────────────────────────────────────────────────────────────────┘
```

See [`docs/architecture.md`](docs/architecture.md) for the full implementation plan.
