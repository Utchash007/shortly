# Shortly — Backend-Only URL Shortener

A production-style backend-only URL shortener: no frontend, no auth. Everything is
exposed through REST APIs and explored via Swagger UI.

```text
                          API Clients (Swagger / curl)
                                     |
                                     v
                          +------------------+
                          |   Spring Boot    |  controllers -> services -> repositories
                          +--------+---------+
                   +---------------+----------------+
                   |               |                |
                   v               v                v
            +------------+  +-------------+  +-------------+
            |   Redis    |  | PostgreSQL  |  | IP Geo API  |
            |  url:{key} |  | urls,clicks |  |  ip:{addr}  |
            +------------+  +-------------+  +-------------+
```

## Stack

Spring Boot 4.1 (Java 25) · Spring Web / Data JPA / Data Redis / Validation /
Actuator · PostgreSQL 16 (source of truth) · Redis 7 (cache-aside) · ZXing (QR) ·
springdoc OpenAPI · JUnit 5 + Mockito + MockMvc + Testcontainers.

## Quickstart (local, no Docker needed)

1. Create `.env` in the project root (never committed):
   ```text
   jdbc=jdbc:postgresql://<host>:5432/postgres?user=<user>&password=<pass>
   redis_conn=redis://default:<pass>@<host>:<port>
   ```
2. Export Spring-style variables from it and boot the dev profile:
   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "dev"
   # SPRING_DATASOURCE_URL / _USERNAME / _PASSWORD from the jdbc line,
   # SPRING_DATA_REDIS_HOST / _PORT / _PASSWORD from redis_conn
   ./mvnw spring-boot:run
   ```
3. Open `http://localhost:8080/swagger-ui.html` (`/v3/api-docs` for raw OpenAPI,
   `/actuator/health` for liveness).

Or run the bundled compose file for local PostgreSQL + Redis:
`docker compose up -d` (defaults in `application.yml` already match it).

## API

| Area      | Method & Path                | Notes                              |
| --------- | ---------------------------- | ---------------------------------- |
| URLs      | `POST /api/urls`             | 201, `{originalUrl, customAlias?, expiresAt?}` |
| URLs      | `GET /api/urls/{id}`         | 200, 404 `URL_NOT_FOUND`           |
| URLs      | `DELETE /api/urls/{id}`      | 204 soft-deactivate, keeps history |
| Redirect  | `GET /{shortCode}`           | 302, 404, 410 `URL_EXPIRED`        |
| Analytics | `GET /api/urls/{id}/analytics` | totals + by-date + by-country    |
| QR        | `GET /api/urls/{id}/qr`      | 200 `image/png`                    |

Error payloads share one shape (`timestamp,status,error,message,path`,
plus `validationErrors` for 400s): `VALIDATION_ERROR`, `INVALID_ALIAS`,
`URL_NOT_FOUND`, `ALIAS_ALREADY_EXISTS`, `URL_EXPIRED`, `INTERNAL_ERROR`.

## How it works

- **Short codes**: 7-char random Base62 (`SecureRandom`), 5 generation retries,
  database `UNIQUE` as the final race guard. A supplied `customAlias` becomes
  the lookup key itself; both share one namespace with reserved-path protection
  (`api`, `actuator`, `swagger-ui`, …).
- **Redirects** follow cache-aside: `GET url:{key}` → JSON
  `{originalUrl,expiresAt,active}`; miss → PostgreSQL → repopulate with
  `TTL = expiresAt - now` (default 1h). Redis outages fall back to the
  database; corrupt entries are evicted.
- **Expiration**: enforced on every redirect (410 Gone) plus an hourly
  `@Scheduled` job that deactivates stale rows and evicts their keys.
  Deletion is soft deletion; analytics survive.
- **Analytics**: each redirect records `{timestamp, ip, country, user-agent,
  referrer}` asynchronously (`@Async` pool, never on the request path).
  Countries come from ip-api with a 30-day `ip:` cache and `UNKNOWN` fallback.
- **QR**: ZXing PNG (256px, EC level M) generated per request, never stored.

## Configuration

| Variable                    | Purpose                              |
| --------------------------- | ------------------------------------ |
| `SPRING_PROFILES_ACTIVE`    | `dev` (ddl `update`) / `prod` (ddl `validate`) |
| `SPRING_DATASOURCE_URL`     | JDBC URL, e.g. Supabase pooler       |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | DB credentials          |
| `SPRING_DATA_REDIS_HOST` / `_PORT` / `_PASSWORD` | Redis |
| `APP_BASE_URL`              | Public host baked into `shortUrl`/QR |
| `PORT`                      | HTTP port (`8080` default, Render-assigned in prod) |

## Testing

```bash
./mvnw test   # 21 unit/slice tests green; 2 Testcontainers suites skip without Docker
```

With Docker running, the integration suites boot real PostgreSQL 16 + Redis 7
containers: full create→302→analytics→QR→delete→410 flow and cache
miss-repopulate behavior.

## Deployment

The Docker image is built outside this repo; `render.yaml` is a Render
Blueprint expecting its URL (`runtime: image`, health check
`/actuator/health`, secrets via `sync: false` dashboard entries). Fill in the
image URL, point `SPRING_DATASOURCE_URL` at Supabase and Redis at a managed
instance, set `SPRING_PROFILES_ACTIVE=prod`, and deploy.

## Layout

```text
src/main/java/com/shortly/app/
  config/      RedisConfig, OpenApiConfig, AsyncConfig
  controller/  UrlController, RedirectController, AnalyticsController, QrCodeController
  dto/         CreateUrlRequest, UrlResponse, AnalyticsResponse, ClickByDate*, ClickByCountry*, ErrorResponse
  entity/      Url, ClickEvent
  repository/  UrlRepository, ClickEventRepository
  service/     UrlService, ShortCodeGenerator, UrlCacheService, UrlCleanupService,
               GeolocationService, AnalyticsService, QrCodeService
  exception/   GlobalExceptionHandler + domain exceptions
```
