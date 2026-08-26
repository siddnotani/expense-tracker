# Expense Tracker — Architecture & Implementation Plan (v2 — Java Spring Boot backend)

Goal: a simple React SPA to track personal expenses, backed by Java Spring Boot microservices behind a BFF.

Changed since v1: backend is Java 21 + Spring Boot 3 instead of Node.js. Frontend, service decomposition, and phasing are unchanged.

## 1. Architecture

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

Why a BFF: the SPA gets one origin, one auth cookie, and page-shaped responses (`GET /api/dashboard` = accounts + recent transactions + month summary in one round trip), while each service stays small and independently deployable.

### Services
| Service | Port | Owns | Key endpoints |
|---|---|---|---|
| bff | 8080 | session cookie, aggregation, request validation, CORS | `/api/auth/*`, `/api/dashboard`, `/api/transactions`, `/api/accounts`, `/api/budgets` |
| auth-service | 8081 | users, BCrypt password hashes, JWT issuing | `POST /signup`, `POST /login`, `GET /verify` |
| accounts-service | 8082 | accounts, cached balance | CRUD `/accounts`, `GET /accounts/{id}/balance` |
| transaction-service | 8083 | transaction ledger | CRUD `/transactions`, `GET /transactions?from&to&categoryId&accountId&page` |
| budget-service | 8084 | categories, monthly budgets, aggregates | CRUD `/categories`, `/budgets`, `GET /summary?month=YYYY-MM` |

Service-to-service auth: the BFF mints a short-lived internal JWT carrying `userId`; every service validates it via Spring Security resource-server config and scopes **every** query by `userId` (no cross-tenant reads).

### Data ownership
- No cross-service DB access. Each service owns its schema (`auth`, `accounts`, `txn`, `budget`) with its own Flyway migrations.
- Balance is derived from the ledger; accounts-service keeps a cached balance updated when transaction-service notifies it. **Decision needed** — synchronous `WebClient` call transaction→accounts (simple, consistent enough for a personal app) vs. an event bus. Recommendation: synchronous now, behind a `BalanceNotifier` interface so swapping in Kafka/Rabbit later is a one-class change.

## 2. Tech choices

**Backend (per service)**
- Java 21, Spring Boot 3.3, Maven multi-module reactor (`pom.xml` at root, one module per service + a `shared` module for DTOs/JWT/error handling).
- Spring Web MVC (blocking, simplest) for the four domain services; `WebClient` in the BFF for fan-out with `Mono.zip` so dashboard calls run in parallel.
- Spring Data JPA + Hibernate, Postgres driver, **Flyway** for migrations.
- Spring Security: BCrypt in auth-service, JWT resource-server validation in the others.
- Bean Validation (`jakarta.validation`) on request DTOs; `@ControllerAdvice` mapping to RFC 7807 `ProblemDetail`.
- Testing: JUnit 5, `@SpringBootTest` + MockMvc, **Testcontainers** Postgres for repository/integration tests, WireMock for BFF fan-out tests.
- Observability: Spring Boot Actuator (`/actuator/health`, `/actuator/info`), structured JSON logging, request-id propagated from the BFF via an interceptor.

**Frontend**
- React 18 + Vite + TypeScript, React Router, TanStack Query for server state, Recharts for the spend-by-category chart, Tailwind for styling. Vite dev proxy `/api` → BFF:8080.

**Local dev**
- `docker compose up` → Postgres + all 5 services (each with a Spring Boot layered Docker image) + the Vite dev server.
- Local run without Docker: `mvn spring-boot:run -pl services/bff` etc., Postgres from compose only.

**Repo layout**
```
pom.xml                    Maven reactor
services/bff
services/auth
services/accounts
services/transaction
services/budget
shared/                    DTOs, JWT utils, error handling, test fixtures
web/                       React SPA (own package.json)
docker-compose.yml
Makefile                   make dev / make test / make lint
```

## 3. Screens (SPA)
1. **Login / Sign up**
2. **Dashboard** — this month's total spend, budget progress bars, spend-by-category donut, last 10 transactions.
3. **Transactions** — filterable/paginated table; add/edit/delete in a modal.
4. **Accounts** — list with balances, add/edit.
5. **Budgets & Categories** — monthly limit per category, actual vs. budget.

## 4. Cross-cutting decisions
- Auth: httpOnly `SameSite=Lax` session cookie between SPA and BFF; JWT only internally. CSRF token on mutations.
- Money as `BigDecimal(19,2)` in JPA and integer minor units (cents) on the wire — never floats.
- Dates as `LocalDate`; all aggregation in the user's timezone, stored UTC.
- Shared DTOs live in `shared/`; the SPA's TS types are generated from the BFF's OpenAPI spec (springdoc) so they can't drift.
- CI (GitHub Actions): `mvn -T1C verify` (compile + tests + Testcontainers), `npm ci && npm run lint && npm run build` for the SPA, Spotless/Checkstyle for Java formatting.
- Seed script: demo user + ~3 months of sample transactions so no screen is ever empty.

## 5. Delivery phases (each = one reviewable PR)
1. **Scaffold** — Maven reactor, 5 Spring Boot skeletons with Actuator health, Postgres + Flyway, docker-compose, Vite SPA shell, GitHub Actions CI.
2. **Auth vertical slice** — auth-service, BFF session handling, JWT plumbing, SPA login/signup + protected routing.
3. **Accounts + transactions** — both services with Flyway migrations, JPA repositories, tests; SPA transactions table and accounts page; seed data.
4. **Budgets, categories & dashboard** — budget-service, BFF `/api/dashboard` parallel fan-out, charts and budget progress in the SPA.
5. **Polish** — empty/error states, responsive layout, OpenAPI-generated TS client, README + `make dev`, e2e smoke test of the golden path.

## 6. Open questions (still need your answers)
1. **Single-user or multi-user?** Multi-user keeps phase 2 as-is; single-user lets me drop auth-service entirely (4 processes instead of 5).
2. **Scope of "banking"** — manual entry only, or also CSV/bank-statement import (and later a Plaid-style sync)?
3. **Service granularity** — the 4 services + BFF above, or collapse accounts/transactions/budgets into one `ledger-service` (BFF + auth + ledger = 3 processes)? For a personal app, 3 is honestly the better tradeoff; say the word and I'll do 5 as specced.
4. **Deployment target** — local Docker only for now, or should I plan for a specific host (AWS/Fly/Render)?
5. **Build tool** — Maven (assumed above) or Gradle?
