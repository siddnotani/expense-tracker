# expense-tracker
Personal expense tracker: React SPA + Java Spring Boot BFF and microservices

## Running locally (BFF)

```bash
cd bff
./mvnw spring-boot:run          # http://localhost:8080
./mvnw -q verify                # build + tests
curl 'http://localhost:8080/api/analytics/summary?from=2026-01-01&to=2026-06-30'
```
