# Shortly — Phased Implementation Plan

This roadmap translates the complete 102-chapter specification in [`url-shortener-backend-only-updated.md`](./url-shortener-backend-only-updated.md) into concrete, actionable implementation phases.

Every phase adheres to the project's agent standards:
- [**`java-springboot`**](.agents/skills/java-springboot/SKILL.md): Constructor injection, immutable `private final` fields, DTO encapsulation, stateless services, `@Transactional` boundaries, Bean Validation (JSR 380), and SLF4J parameterized logging.
- [**`java-docs`**](.agents/skills/java-docs/SKILL.md): Complete Javadoc coverage on all public and protected classes, records, and methods.

---

## Roadmap & Progress Tracker

| Phase | Milestone | Core Components | Spec Chapters | Status |
| :---: | :--- | :--- | :--- | :---: |
| **0** | **Repository & Project Setup** | Repo clone/init, Maven `pom.xml`, Spring Boot 4.1.x, `application.yml` | Ch. 1–8 | 🔲 Pending |
| **1** | **Domain Modeling & Persistence** | `Url` entity, `UrlRepository`, PostgreSQL schema & indexes | Ch. 9–16 | 🔲 Pending |
| **2** | **Core Shortening Engine & Basic API** | Base62/random token generator, `CreateUrlRequest`, `UrlResponse`, `UrlService`, `UrlController` | Ch. 17–28 | 🔲 Pending |
| **3** | **Redis Cache-Aside Layer** | `RedisConfig`, cache-aside read path, TTL, graceful fallback | Ch. 29–34, 74–75 | 🔲 Pending |
| **4** | **Custom Aliases & Guardrails** | Custom alias regex validation, reserved path checks, collision handling | Ch. 20, 23, 76 | 🔲 Pending |
| **5** | **Lifecycle, Expiration & Scheduled Cleanup** | Expiration checks, HTTP 410 Gone, `@Scheduled` cleanup task, cache purge | Ch. 21–22, 35–38 | 🔲 Pending |
| **6** | **Analytics & IP Geolocation Pipeline** | `ClickEvent` entity, client IP extractor, IP-to-Country lookup, analytics endpoints | Ch. 39–47, 77–78 | 🔲 Pending |
| **7** | **QR Code Generation** | ZXing integration, `QrCodeService`, PNG byte stream endpoint | Ch. 48–50, 59 | 🔲 Pending |
| **8** | **OpenAPI / Swagger & Global Resilience** | Springdoc OpenAPI config, `@ControllerAdvice` global error handler | Ch. 54–58, 60–64 | 🔲 Pending |
| **9** | **Automated Testing Suite** | JUnit 5, Mockito unit tests, `@WebMvcTest`, Testcontainers (Postgres & Redis) | Ch. 66–71 | 🔲 Pending |
| **10** | **Production Deployment & Docker** | Dockerfile, Render + Supabase configuration, environment variables, health checks | Ch. 82–91, 98–101 | 🔲 Pending |

---

## Phase 0: Repository & Project Initialization

### Objectives
Initialize the codebase, configure build tools, declare all core dependencies, and establish configuration profiles.

### Relevant Spec Chapters
- **Chapters 1–8**: Tech stack, dependencies, database config.

### Skill Directives
- Standard package root: `com.shortly.app`.
- Build with JDK 27 targeting Java 25 (`<java.version>25</java.version>` / `--release 25`).
- Externalize all database credentials and hosts using `application.yml` and environment variables.

### Target Files to Create
- `pom.xml` (`spring-boot-starter-parent:4.1.x`, `<java.version>25</java.version>`)
- `mvnw`, `mvnw.cmd`, `.mvn/` (Maven wrapper — required for Phase 9/10 `./mvnw` commands)
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `src/main/java/com/shortly/app/ShortlyApplication.java`
- `docker-compose.yml` (local PostgreSQL 16 + Redis 7)
- `.gitignore`

### Key Dependencies (`pom.xml`)
- Parent: `org.springframework.boot:spring-boot-starter-parent:4.1.x` (Spring Framework 7, Jakarta EE 11, Servlet 6.1 / Tomcat 11 baseline).
- Properties: `<java.version>25</java.version>` — build with local JDK 27 via `--release 25`.
- Managed by Boot 4.1 BOM: Hibernate 7.1, Jackson 3, Hibernate Validator 9, Lettuce 6.8 / Jedis 6.2, Testcontainers 2.0, Mockito 5.20. Do not pin these manually.
- Embedded server: Tomcat 11 (Undertow is dropped in Boot 4 — do not add it).

```xml
<!-- Spring Boot 4.1.x Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Database Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- QR Code Generation -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>

<!-- OpenAPI Documentation (Boot 4 requires springdoc 3.x) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.1.1</version>
</dependency>

<!-- Testing (Boot 4.1 BOM manages versions; Testcontainers 2.0) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Verification Criteria
- [ ] `./mvnw spring-boot:run` starts without errors on `http://localhost:8080` (JDBC uses `:5432` — spec Ch. 8 `:3306` is a MySQL-port typo).
- [ ] Docker Compose boots PostgreSQL on `:5432` and Redis on `:6379`.
- [ ] Actuator health check at `GET /actuator/health` returns `{"status":"UP"}` (`management.endpoints.web.exposure.include=health`).

### Commit Checkpoint
`git commit -m "chore: initialize Spring Boot 4.1 project with PostgreSQL and Redis dependencies"`

---

## Phase 1: Domain Modeling & Persistence Layer

### Objectives
Define the primary URL entity with database-level constraints, unique indexing, and JPA repository abstractions.

### Relevant Spec Chapters
- **Chapters 9, 10, 12, 13, 15**: URLs table design, JPA mapping, repository query methods.

### Skill Directives
- Use constructor or builder patterns where appropriate; maintain entity encapsulation.
- Document all entity fields and repository methods with descriptive Javadocs.

### Target Files to Create
- `src/main/java/com/shortly/app/entity/Url.java`
- `src/main/java/com/shortly/app/repository/UrlRepository.java`

### Implementation Specs
- **Table Name**: `urls` (PostgreSQL; JPA `GenerationType.IDENTITY` — spec Ch. 10/11 `AUTO_INCREMENT` is MySQL syntax, do not use raw).
- **Fields**:
  - `id`: `Long` (Primary Key, `@GeneratedValue(strategy = GenerationType.IDENTITY)`)
  - `originalUrl`: `String` (`@Column(nullable = false, length = 2048)`)
  - `shortCode`: `String` (`@Column(nullable = false, unique = true, length = 32)`, generated 7 chars within 6–8 range)
  - `customAlias`: `String` (`@Column(unique = true, length = 30)`, nullable — matches Phase 4 regex `^[a-zA-Z0-9-_]{3,30}$`; NULLs do not conflict in PostgreSQL)
  - `createdAt`: `Instant` (`@Column(nullable = false, updatable = false)`)
  - `expiresAt`: `Instant` (nullable)
  - `active`: `boolean` (default `true`)
- **Indexes**:
  - Unique index on `short_code` (DB is final anti-collision guard; catch `DataIntegrityViolationException` on race)
  - Unique index on `custom_alias`
  - Index on `expires_at` (for cleanup query efficiency)
  - Index on `active`
- **Repository Methods**:
  - `Optional<Url> findByShortCode(String shortCode);`
  - `Optional<Url> findByCustomAlias(String customAlias);`
  - `default Optional<Url> resolve(String codeOrAlias)` -> try `findByShortCode` then `findByCustomAlias` (single redirect lookup; see Phase 4).
  - `boolean existsByShortCode(String shortCode);`
  - `boolean existsByCustomAlias(String customAlias);`
  - `List<Url> findByExpiresAtBeforeAndActiveTrue(Instant now);` (required by Phase 5 cleanup — do not omit).

### Verification Criteria
- [ ] Hibernate auto-generates or migrates the `urls` table with correct constraints and indexes.
- [ ] Unit/repository test confirms insert and query operations by short code.

### Commit Checkpoint
`git commit -m "feat(domain): add Url entity, constraints, and UrlRepository"`

---

## Phase 2: Core Shortening Engine & Basic API

### Objectives
Implement Base62/random short-code generation with collision resilience, DTO validation, URL creation endpoint, and redirect logic directly from PostgreSQL.

### Relevant Spec Chapters
- **Chapters 17–28**: Token generation, collision loops, DTOs, `UrlService`, `POST /api/urls`, `GET /{shortCode}`.

### Skill Directives
- Constructor injection with `private final` fields for `UrlService` and `UrlController`.
- Never expose `Url` JPA entities directly; map to `UrlResponse` record/DTO.
- Use `@Valid` and Bean Validation: `originalUrl` (`@NotBlank` + Hibernate `@URL` + `@Pattern(regexp = "^https?://.+")` to allow only http/https and reject `javascript:`/`data:`/`file:`), `customAlias` (`@Pattern(regexp = "^[a-zA-Z0-9-_]{3,30}$")`), `expiresAt` (`@Future`).

### Target Files to Create
- `src/main/java/com/shortly/app/service/ShortCodeGenerator.java`
- `src/main/java/com/shortly/app/dto/CreateUrlRequest.java`
- `src/main/java/com/shortly/app/dto/UrlResponse.java`
- `src/main/java/com/shortly/app/service/UrlService.java`
- `src/main/java/com/shortly/app/controller/UrlController.java` (`POST /api/urls`, `GET /api/urls/{id}`, `DELETE /api/urls/{id}`)
- `src/main/java/com/shortly/app/controller/RedirectController.java` (`GET /{shortCode}`)
- `src/main/java/com/shortly/app/exception/UrlNotFoundException.java`
- `src/main/java/com/shortly/app/exception/AliasAlreadyExistsException.java` (front-loaded: creation must enforce 409 from day one; Phase 4 adds reserved-word rules)
- `src/main/java/com/shortly/app/exception/UrlExpiredException.java` (front-loaded: redirect enforces `active`/`expiresAt` immediately; Phase 5 adds scheduled cleanup)

### Key Implementation Details
- **Generator**: default 7 characters within 6–8 range, alphanumeric (`[a-zA-Z0-9]`) using `SecureRandom`.
- **Collision Retry**: Maximum 5 retry attempts on `existsBy*` check; `UNIQUE(short_code)` is the final guard — catch `DataIntegrityViolationException` and retry/return 409.
- **Endpoints**:
  - `POST /api/urls` -> 201 Created with JSON `UrlResponse`.
  - `GET /api/urls/{id}` -> 200 with `UrlResponse`.
  - `DELETE /api/urls/{id}` -> 204 (soft-deactivate: `active = false`, evict cache, keep analytics).
  - `GET /{shortCode}` -> 302 Found with `Location: <originalUrl>` (also resolves custom aliases via Phase 1 `resolve()`).

### Verification Criteria
```bash
# 1. Create short URL
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://spring.io"}'
# Response: 201 Created with shortCode e.g. "xK9a1Z"

# 2. Test Redirect
curl -I http://localhost:8080/xK9a1Z
# Response: HTTP/1.1 302 Found, Location: https://spring.io
```

### Commit Checkpoint
`git commit -m "feat(api): implement short-code generation, URL creation, and HTTP redirect"`

---

## Phase 3: Redis Cache-Aside Layer

### Objectives
Accelerate URL resolution to sub-millisecond response times using Redis cache-aside read-through caching, configure TTL, and handle cache failures gracefully.

### Relevant Spec Chapters
- **Chapters 29–34, 74–75**: Cache-aside pattern, key design, fallback architecture, cache stampede prevention.

### Skill Directives
- Encapsulate Redis operations in a dedicated `UrlCacheService` or integration layer.
- Use SLF4J parameterized logging to log cache misses and Redis connection warnings without crashing requests.

### Target Files to Create / Modify
- `src/main/java/com/shortly/app/config/RedisConfig.java`
- `src/main/java/com/shortly/app/service/UrlCacheService.java`
- [MODIFY] `src/main/java/com/shortly/app/service/UrlService.java`

### Key Implementation Details
- **Key Format**: `url:{codeOrAlias}` -> JSON value `{originalUrl, expiresAt, active}` (Jackson 3 via Boot 4.1 BOM; `StringRedisTemplate` with JSON string). Never cache the raw URL string alone — expiration/`active` checks would be bypassed (spec Ch. 30).
- **TTL**: if `expiresAt` present, `TTL = expiresAt - now` (do not outlive the URL); else default 1 hour (configurable via `application.yml`, e.g. `app.cache.default-ttl: 3600s`).
- **Read Path**:
  1. Check Redis for `url:{codeOrAlias}` (single key per lookup value; see Phase 4).
  2. If Cache HIT -> validate `active`/`expiresAt` from cached JSON, return directly (skip DB query).
  3. If Cache MISS -> Query PostgreSQL via `resolve()`.
  4. If found in DB -> Write JSON to Redis with computed TTL, return URL.
  5. If not found -> throw `UrlNotFoundException` (404).
- **Resilience**: If Redis is unreachable, log warning with SLF4J and transparently fall back to PostgreSQL.

### Verification Criteria
- [ ] Inspect Redis CLI (`KEYS url:*` and `GET url:<codeOrAlias>`) after first redirect — value is JSON with `originalUrl`/`expiresAt`/`active`.
- [ ] Second redirect executes without hitting the database (verify Hibernate SQL log shows no `SELECT`).
- [ ] Stop Redis container (`docker stop redis`); redirect continues to function via DB fallback.

### Commit Checkpoint
`git commit -m "feat(cache): integrate Redis cache-aside pattern with graceful DB fallback"`

---

## Phase 4: Custom Aliases & Validation Guardrails

### Objectives
Allow users to specify custom readable aliases (e.g., `/my-portfolio`) while validating character safety, reserved system paths, and preventing race collisions.

### Relevant Spec Chapters
- **Chapters 20, 23, 76**: Custom alias validation, reserved keywords, collision handling.

### Target Files to Create / Modify
- `src/main/java/com/shortly/app/exception/InvalidAliasException.java` (`AliasAlreadyExistsException` already exists from Phase 2)
- [MODIFY] `src/main/java/com/shortly/app/dto/CreateUrlRequest.java`
- [MODIFY] `src/main/java/com/shortly/app/service/UrlService.java`

### Key Implementation Details
- **Alias Validation Rules**:
  - Regex: `^[a-zA-Z0-9-_]{3,30}$` (3 to 30 characters, alphanumeric, hyphen, underscore; DB column `length = 30` per Phase 1).
  - Reserved Words (case-insensitive, block exact match): `api`, `actuator`, `swagger-ui`, `swagger-ui.html`, `v3`, `v3/api-docs`, `favicon.ico`, `health`, `error`.
- **Database & Cache Sync** (single-namespace resolution):
  - `shortCode` and `customAlias` share the `GET /{codeOrAlias}` namespace — reject an alias that already exists as a `shortCode` and vice versa.
  - Check `existsByCustomAlias`/`existsByShortCode` before insert, but rely on DB `UNIQUE` + catch `DataIntegrityViolationException` -> 409 (race guard).
  - Cache single key `url:{codeOrAlias}` with JSON value on demand (do not dual-write `url:{shortCode}` + `url:{customAlias}` — causes stale twins). Evict the looked-up key plus the entity's `shortCode`/`customAlias` keys on update/deactivate.

### Verification Criteria
```bash
# 1. Custom alias creation
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com","customAlias":"my-github"}'
# Response: 201 Created

# 2. Duplicate alias attempt
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://google.com","customAlias":"my-github"}'
# Response: 409 Conflict

# 3. Reserved keyword rejection
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://google.com","customAlias":"api"}'
# Response: 400 Bad Request
```

### Commit Checkpoint
`git commit -m "feat(alias): support custom aliases with reserved keyword protection and uniqueness checks"`

---

## Phase 5: Lifecycle, Expiration & Scheduled Cleanup

### Objectives
Support optional time-to-live (`expiresAt`) on short URLs, return HTTP 410 Gone for expired links, and automate background cleanup via `@Scheduled`.

### Relevant Spec Chapters
- **Chapters 21–22, 35–38**: Expiration checks, 410 Gone, `@Scheduled` cleanup worker, cache invalidation.

### Target Files to Create / Modify
- `src/main/java/com/shortly/app/service/UrlCleanupService.java` (spec calls it `CleanupService` — this explicit name wins; `UrlExpiredException` already exists from Phase 2)
- [MODIFY] `src/main/java/com/shortly/app/service/UrlService.java` (check `active` + `expiresAt` in both DB and cached-JSON paths)
- [MODIFY] `src/main/java/com/shortly/app/ShortlyApplication.java` (enable `@EnableScheduling`)

### Key Implementation Details
- **Request Field**: `expiresAt` (ISO-8601 UTC timestamp). Must be in the future (`@Future` rejects past at validation; service double-checks).
- **Redirect Path**: If `!url.isActive()` or (`url.getExpiresAt() != null && url.getExpiresAt().isBefore(Instant.now())`), throw `UrlExpiredException` (maps to HTTP 410 Gone in Phase 8). Apply to cached JSON too.
- **Scheduled Background Job**:
  - Cron: top of every hour (`@Scheduled(cron = "0 0 * * * *")`). Spec Ch. 37 `fixedRate = 300000` (5 min) is dev-only — too chatty for prod DB.
  - Queries: via Phase 1 `findByExpiresAtBeforeAndActiveTrue(now)`, then soft-deactivate (`active = false`, keep row + analytics; hard delete is out of scope).
  - Cache Eviction: evict `url:{shortCode}` and (if present) `url:{customAlias}` for each deactivated row.

### Verification Criteria
- [ ] Create a URL with `expiresAt` set to 5 seconds in the future.
- [ ] Initial redirect returns `302 Found`.
- [ ] Request after 6 seconds returns `410 Gone`.
- [ ] Cleanup service test confirms expired records are deactivated.

### Commit Checkpoint
`git commit -m "feat(lifecycle): implement URL expiration, HTTP 410 response, and scheduled cleanup job"`

---

## Phase 6: Click Analytics & Geolocation Pipeline

### Objectives
Track redirect events, extract client IP and User-Agent, resolve client countries via IP geolocation, cache IP-country lookups, and expose aggregated metrics.

### Relevant Spec Chapters
- **Chapters 11, 14, 16, 39–47, 77–78**: `ClickEvent` entity, client IP resolution, IP-API integration, analytics aggregation queries.

### Skill Directives
- Keep the redirect response path ultra-fast: record clicks asynchronously via `@Async` (add `@EnableAsync` + `TaskExecutor` in `AsyncConfig`, or reuse `ShortlyApplication`). Never block redirect on geo-IP.
- Use DTO projections (`ClickByDateResponse(date, clicks)`, `ClickByCountryResponse(country, clicks)`, `AnalyticsResponse`) — field name is `clicks` to match spec Ch. 43/44.

### Target Files to Create
- `src/main/java/com/shortly/app/entity/ClickEvent.java` (fields: `id`, `url` (`@ManyToOne LAZY`), `clickedAt`, `ipAddress`, `country`, `userAgent`, `referrer` — `country` matches spec Ch. 14, `UNKNOWN` fallback)
- `src/main/java/com/shortly/app/repository/ClickEventRepository.java` (add `long countByUrlId(Long urlId)`, `@Query` `countGroupedByDate(urlId)`, `@Query` `countGroupedByCountry(urlId)`)
- `src/main/java/com/shortly/app/service/GeolocationService.java`
- `src/main/java/com/shortly/app/service/AnalyticsService.java`
- `src/main/java/com/shortly/app/dto/AnalyticsResponse.java`
- `src/main/java/com/shortly/app/dto/ClickByDateResponse.java`
- `src/main/java/com/shortly/app/dto/ClickByCountryResponse.java`
- `src/main/java/com/shortly/app/controller/AnalyticsController.java`
- `src/main/java/com/shortly/app/util/HttpRequestUtil.java` (parses `X-Forwarded-For`, `User-Agent`)
- `src/main/java/com/shortly/app/config/AsyncConfig.java` (`@EnableAsync` executor)

### Key Implementation Details
- **ClickEvent Fields**: `id`, `url_id`, `clickedAt`, `ipAddress`, `country`, `userAgent`, `referrer`.
- **IP Extraction**: take first IP from `X-Forwarded-For` only when behind trusted proxy (Render/Nginx); else `X-Real-IP`, fallback `request.getRemoteAddr()`. Never blindly trust client-supplied headers (spec Ch. 40).
- **Geolocation Service**: Query `http://ip-api.com/json/{ip}?fields=countryCode` with 2s timeout; on failure/timeout return `UNKNOWN` without failing redirect. Cache results in Redis: `ip:{ipAddress}` -> `country` (30-day TTL).
- **Aggregated Endpoint**:
  - `GET /api/urls/{id}/analytics` -> Returns total clicks, clicks grouped by date, and top countries.

### Verification Criteria
- [ ] Trigger multiple redirects with varying `X-Forwarded-For` and `User-Agent` headers.
- [ ] Query `GET /api/urls/{id}/analytics`:
  ```json
  {
    "totalClicks": 12,
    "clicksByDate": [ { "date": "2026-09-22", "clicks": 12 } ],
    "clicksByCountry": [ { "country": "US", "clicks": 8 }, { "country": "DE", "clicks": 4 } ]
  }
  ```

### Commit Checkpoint
`git commit -m "feat(analytics): add click tracking, IP geolocation caching, and analytics aggregation API"`

---

## Phase 7: QR Code Generation

### Objectives
Generate and stream high-resolution PNG QR codes for any shortened URL directly to clients via ZXing.

### Relevant Spec Chapters
- **Chapters 23, 48–50, 59**: ZXing integration, byte stream streaming, QR API endpoints.

### Target Files to Create
- `src/main/java/com/shortly/app/service/QrCodeService.java`
- `src/main/java/com/shortly/app/controller/QrCodeController.java` (spec calls it `QrController` — this explicit name wins)

### Key Implementation Details
- **Service**: Generates 256x256 QR matrix with error correction level `M`, rendered to PNG `byte[]` (ZXing 3.5.3, generate on demand — no DB storage).
- **Endpoint**: `GET /api/urls/{id}/qr` returning `MediaType.IMAGE_PNG_VALUE` only. Do NOT add `GET /{shortCode}/qr` — it collides with `GET /{shortCode}` redirect.
- **Content**: Encodes `${APP_BASE_URL}/{shortCode-or-alias}` where `APP_BASE_URL` comes from Phase 10 env (`https://shortly-api.onrender.com` prod, `http://localhost:8080` dev).

### Verification Criteria
```bash
curl -o qr.png http://localhost:8080/api/urls/1/qr
# File is valid PNG image that decodes to the short URL
```

### Commit Checkpoint
`git commit -m "feat(qr): implement ZXing QR code generation and streaming endpoint"`

---

## Phase 8: OpenAPI / Swagger Documentation & Global Resilience

### Objectives
Deliver an interactive, fully documented Swagger UI and implement a centralized `@ControllerAdvice` error handling system conforming to standard error schemas.

### Relevant Spec Chapters
- **Chapters 54–58, 60–64**: OpenAPI 3 metadata, tags, examples, `@ControllerAdvice`, standard error response.

### Skill Directives
- Follow [**`java-springboot`**](.agents/skills/java-springboot/SKILL.md) guidelines for consistent error payloads and descriptive HTTP status mapping.
- Follow [**`java-docs`**](.agents/skills/java-docs/SKILL.md) to annotate controllers and configuration beans.

### Target Files to Create / Modify
- `src/main/java/com/shortly/app/config/OpenApiConfig.java`
- `src/main/java/com/shortly/app/dto/ErrorResponse.java`
- `src/main/java/com/shortly/app/exception/GlobalExceptionHandler.java`
- `src/main/java/com/shortly/app/exception/UrlNotFoundException.java` (created in Phase 2; handled here)
- [MODIFY] Annotate all controllers with `@Tag`, `@Operation`, and `@ApiResponse`.

### Standard Error Response Schema
```java
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> validationErrors // null except for bean-validation failures; base fields match spec Ch. 61
) {}
```

### Exception Mapping Table
| Exception | HTTP Status | Response Scenario |
| :--- | :---: | :--- |
| `MethodArgumentNotValidException` / `ConstraintViolationException` | 400 Bad Request | Payload validation failures (`validationErrors` populated) |
| `HttpMessageNotReadableException` / `MethodArgumentTypeMismatchException` | 400 Bad Request | Malformed JSON / wrong param types |
| `InvalidAliasException` | 400 Bad Request | Reserved word or illegal characters |
| `UrlNotFoundException` | 404 Not Found | Non-existent short code |
| `AliasAlreadyExistsException` / `DataIntegrityViolationException` | 409 Conflict | Duplicate alias/shortCode (incl. race) |
| `UrlExpiredException` | 410 Gone | Link past expiration date |
| `Exception` (catch-all) | 500 Internal Server Error | Unhandled runtime errors |

### Verification Criteria
- [ ] Access Swagger UI at `http://localhost:8080/swagger-ui.html` (redirects to `/swagger-ui/index.html` on springdoc 3.x).
- [ ] Trigger invalid requests (empty URL, duplicate alias) and verify error payload structure matches `ErrorResponse`.

### Commit Checkpoint
`git commit -m "docs(api): add OpenAPI 3 documentation and global exception handler"`

---

## Phase 9: Automated Testing Suite

### Objectives
Build a test suite ensuring reliability across domain logic, controller validation, caching, and database transactions using Testcontainers.

### Relevant Spec Chapters
- **Chapters 66–71**: Unit tests, controller slice tests, Testcontainers integration tests, Redis tests.

### Skill Directives
- Use `@WebMvcTest` for controller validation without booting full context.
- Use Testcontainers (`PostgreSQLContainer` from `org.testcontainers:testcontainers-postgresql` and `GenericContainer("redis:7")`) for integration tests. Versions come from Boot 4.1 BOM (Testcontainers BOM 2.0.5) declared in Phase 0 — do not re-pin. Note: Testcontainers 2.x renamed modules with a `testcontainers-` prefix (`postgresql` -> `testcontainers-postgresql`, `junit-jupiter` -> `testcontainers-junit-jupiter`); the old 1.x coordinates do not resolve.

### Target Files to Create
- `src/test/java/com/shortly/app/service/ShortCodeGeneratorTest.java`
- `src/test/java/com/shortly/app/service/UrlServiceTest.java`
- `src/test/java/com/shortly/app/controller/UrlControllerTest.java`
- `src/test/java/com/shortly/app/integration/AbstractIntegrationTest.java` (shared `@ServiceConnection` containers + `application-test.yml`)
- `src/test/java/com/shortly/app/integration/UrlShortenerIntegrationTest.java`
- `src/test/java/com/shortly/app/integration/RedisCacheIntegrationTest.java`
- `src/test/resources/application-test.yml`

### Key Test Scenarios
1. **ShortCodeGeneratorTest**: Validate default length 7 (range 6–8), charset `[a-zA-Z0-9]`, and uniqueness across 10,000 generations (62^7 space — collision indicates RNG bug, not a flakiness allowance).
2. **UrlServiceTest**: Verify cache hit skips DB, cache miss writes JSON to Redis, expired/inactive URL throws `UrlExpiredException`.
3. **UrlControllerTest**: MockMvc validates 400 Bad Request for malformed URLs, empty bodies, and reserved aliases.
4. **Integration Tests (Testcontainers)**: Full end-to-end flow creating a URL, executing a redirect, validating click event count in PostgreSQL and key existence in Redis.

### Verification Criteria
- [ ] Run `./mvnw clean test` (use wrapper committed in Phase 0) — 100% tests pass.

### Commit Checkpoint
`git commit -m "test: add unit tests, MockMvc controller tests, and Testcontainers integration suite"`

---

## Phase 10: Production Deployment (Render + Supabase) & Docker

### Objectives
Package the application into an optimized Docker container, configure production profiles for Supabase PostgreSQL and Render Redis, and set up deployment scripts.

### Relevant Spec Chapters
- **Chapters 82–91, 98–101**: Render web service, Supabase connection pooler, environment variables, health checks.

### Target Files to Create
- `Dockerfile` (Multi-stage build using Eclipse Temurin 25)
- `.dockerignore`
- `render.yaml` (Infrastructure-as-code specification for Render)
- `README.md` (Production portfolio documentation with architecture diagrams)
- Phase 0 must additionally commit Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`) — Phase 10 build uses `./mvnw clean package -DskipTests`.

### Production Environment Variables (canonical — map 1:1 in `application-prod.yml`)
| Variable | Description | Example |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active profile | `prod` |
| `SPRING_DATASOURCE_URL` | Supabase JDBC URL (`spring.datasource.url`; use `:5432`, not spec Ch. 8 `:3306` typo) | `jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Supabase DB user | `postgres.your-project-ref` |
| `SPRING_DATASOURCE_PASSWORD` | Supabase DB password | `********` |
| `SPRING_DATA_REDIS_HOST` | Render Redis host | `red-xxxx.render.com` |
| `SPRING_DATA_REDIS_PORT` | Render Redis port | `6379` |
| `SPRING_DATA_REDIS_PASSWORD` | Render Redis password | `********` |
| `APP_BASE_URL` | Public application host (maps to `app.base-url` for QR + `UrlResponse.shortUrl`) | `https://shortly-api.onrender.com` |

Do not use legacy `DATABASE_URL`/`DB_USERNAME`/`BASE_URL` names — they do not bind without extra mapping.
- `server.port: ${PORT:8080}` (Render-assigned port, local 8080 fallback).
- `management.endpoints.web.exposure.include: health` so Render health check `GET /actuator/health` returns `{"status":"UP"}`.
- Prod JPA: `ddl-auto: validate` (never `update` in prod; adopt Flyway/Liquibase next — spec Ch. 83 warning).

### Verification Criteria
- [ ] `docker build -t shortly .` successfully builds image.
- [ ] `docker run -p 8080:8080 shortly` starts with prod profile.
- [ ] Health check returns healthy on Render.

### Commit Checkpoint
`git commit -m "ci/cd: add Dockerfile, Render deployment config, and production README"`

---

## Execution Guide

When ready to begin implementation:
1. Initialize Phase 0 (`pom.xml` and initial project layout).
2. Work incrementally phase by phase.
3. At each phase, verify against the listed **Verification Criteria** before proceeding to the next.
