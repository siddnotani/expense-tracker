# expense-tracker
Personal expense tracker: React SPA + Java Spring Boot BFF and microservices

## Running locally

### BFF (Spring Boot, `bff/`)

```bash
cd bff
./mvnw spring-boot:run      # http://localhost:8080 (H2 console at /h2-console, JDBC URL jdbc:h2:mem:expense_tracker, user `sa`)
./mvnw verify               # build + tests
```

The BFF uses an in-memory H2 database (PostgreSQL mode) with Flyway migrations and seed data under `bff/src/main/resources/db/migration/`.
