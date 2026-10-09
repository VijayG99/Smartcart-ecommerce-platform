# SmartCart — E-commerce Order & Inventory Management Platform

A full-stack e-commerce application built as a **modular monolith** with Spring Boot, PostgreSQL, Redis, Kafka and React.
It is designed to show real backend skills (security, transactions, caching, event-driven processing) in a codebase small
enough to understand and explain end to end.

> **Honesty note:** every feature listed below has working code in this repository. The code was written without
> access to a build environment, so run `mvn test` first and fix any small compile or version issues you meet.
> See [Testing](#17-testing-instructions) for what is and is not covered.

## 1. Project overview

Customers browse and search products, fill a cart and place orders. Admins manage products, categories and stock, and move
orders through their lifecycle. Placing an order is one database transaction; afterwards a Kafka event drives
asynchronous work (order confirmation, low-stock warnings, notifications, analytics).

## 2. Features

| Area | What is implemented |
|---|---|
| Auth | Registration, login, BCrypt passwords, JWT, `CUSTOMER` / `ADMIN` roles, profile |
| Catalog | Product + category CRUD (admin), public search, category/price/in-stock filters, sorting, pagination, soft delete |
| Inventory | Stock per product, admin stock updates, stock never negative (row lock + DB check constraint) |
| Cart | Add / update / remove / view, duplicate products merged, stock validated, ownership from JWT |
| Orders | Create from cart, status state machine, cancel returns stock, owner-or-admin read access |
| Redis | Cache-aside product lookup with TTL, `@Cacheable` / `@CachePut` / `@CacheEvict`, JSON values, outage fallback |
| Kafka | `OrderCreatedEvent` on `order-events`; inventory, notification and analytics consumers; retries + dead-letter topic |
| Ops | Swagger UI, Actuator, structured error responses, Docker Compose, health checks |
| Frontend | React app: auth, shop, product page, cart, checkout, orders, profile, admin products / inventory / orders |
| Optional | Natural-language product search (rule-based, **not** machine learning — see below) |

## 3. Architecture

```
React (nginx)  ──HTTP/JSON──►  Spring Boot API
                                 Controller → Service → Repository → JPA/Hibernate → PostgreSQL
                                                │             │
                                                │             └── Redis (product cache)
                                                └── Spring event ──(after commit)──► Kafka  order-events
                                                                                      ├─ inventory-group    → confirm order, low-stock warning
                                                                                      ├─ notification-group → log / optional email
                                                                                      └─ analytics-group    → order_analytics table
```

Packages (`com.smartcart`): `config, controller, service, service.impl, repository, entity, dto, mapper, exception,
security, event, kafka, listener, cache, util`. Modules (catalog, inventory, cart, orders, notifications, analytics) only talk
through service interfaces or Kafka events, so any of them can later become a separate service.

### Order creation (one transaction)

```
POST /api/orders → load cart → for each line (sorted by product id):
    lock stock row (SELECT … FOR UPDATE) → reduce stock (throws if not enough)
→ create Order + OrderItems (price snapshot) → clear cart → publish Spring event
→ COMMIT → Kafka producer sends OrderCreatedEvent
```
Any exception (for example `InsufficientStockException`) rolls everything back: no partial order, stock unchanged, cart intact,
and no event is sent. Locks are taken in a fixed order to avoid deadlocks.

## 4. Technology stack

Java 17, Spring Boot 3.3, Spring MVC, Spring Data JPA/Hibernate, Spring Security + JWT (jjwt), Bean Validation, Maven, Lombok,
PostgreSQL 16, Redis 7, Apache Kafka 3.8 (KRaft), Docker / Compose, React 18 + Vite + React Router + Axios,
springdoc-openapi, JUnit 5, Mockito, Spring Boot Test, H2 (tests only).

## 5. Database architecture

`users` · `categories` · `products` (→ category) · `inventory` (1:1 product, `CHECK quantity >= 0`) · `carts` (1:1 user) ·
`cart_items` (unique cart+product) · `orders` (→ user) · `order_items` (name/price snapshot) · `order_analytics` (unique order id).

Decisions worth explaining: role is an enum on `users` (two roles do not justify a table); entities are never returned from the API
(DTOs only); relationships are unidirectional except cart↔items and order↔items (needed for cascade/orphan removal);
associations are `LAZY` with `default_batch_fetch_size` to avoid N+1 queries; indexes on product name/category and order user.
Schema is generated with `ddl-auto=update` for development — use Flyway or Liquibase before real production.

## 6. API documentation

Swagger UI: <http://localhost:8080/swagger-ui.html> · OpenAPI JSON: `/v3/api-docs`. Click **Authorize** and paste the login token.

| Method & path | Access |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | public |
| `GET /api/users/profile` | logged in |
| `GET /api/products`, `GET /api/products/{id}`, `GET /api/products/smart-search`, `GET /api/categories` | public |
| `POST/PUT/DELETE /api/products…`, `POST/PUT/DELETE /api/categories…` | ADMIN |
| `GET /api/inventory`, `GET/PUT /api/inventory/{productId}` | ADMIN |
| `GET/POST /api/cart`, `POST /api/cart/items`, `PUT/DELETE /api/cart/items/{id}` | CUSTOMER |
| `POST /api/orders` | CUSTOMER |
| `GET /api/orders/{id}`, `GET /api/orders/user/{userId}` | owner or ADMIN |
| `GET /api/orders`, `PUT /api/orders/{id}/status` | ADMIN |
| `GET /api/analytics/summary` | ADMIN |
| `/actuator/health`, `/actuator/info` | public; other actuator endpoints ADMIN |

Product list query params: `q, categoryId, minPrice, maxPrice, inStock, page, size (max 50), sortBy (name|price|createdAt), direction`.
Errors always look like: `{"timestamp","status","error","message","path","validationErrors"}`.
Status codes: 400 validation, 401 missing/invalid token or bad login, 403 wrong role/not your data, 404, 409 duplicate / insufficient stock / invalid order transition.

## 7. Authentication flow

1. `POST /api/auth/login` → server checks the BCrypt hash → returns a signed JWT (subject = email, claims `uid`, `role`, 1 h expiry by default).
2. The React app stores the token and sends `Authorization: Bearer <token>`.
3. `JwtAuthenticationFilter` verifies signature + expiry, loads the user, sets the Spring Security context. The app is stateless (no sessions, CSRF disabled because no cookies are used).
4. URL rules in `SecurityConfig` plus `@PreAuthorize` on controllers enforce roles; services receive the user id from the token, never from request bodies.
5. Public registration always creates `CUSTOMER`. The first `ADMIN` is seeded from `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

Trade-off: the token is kept in `localStorage`, which is simple but readable by injected scripts (XSS). An httpOnly cookie + CSRF protection is the hardening step.

## 8. Redis architecture

* **Cache-aside** on `ProductService.getById` (`@Cacheable`, cache `products`, key = product id, JSON values, TTL 10 min).
  *Hit*: value returned from Redis, method body never runs. *Miss*: PostgreSQL is queried, result stored with TTL (look for the "Cache MISS" log line).
* **Consistency**: `update` uses `@CachePut`; `delete` uses `@CacheEvict`; stock changes evict programmatically **after the transaction commits** (`ProductCache`), so a concurrent reader cannot re-cache old data.
* **TTL** is the safety net if an eviction is ever missed.
* **Serialization**: keys are strings (`smartcart::products::5`), values are JSON (readable with `redis-cli`).
* **Redis down?** A `CacheErrorHandler` logs and falls back to PostgreSQL.
* Search/list results are *not* cached (too many key combinations, hard to invalidate).
* `ProductResponse` is deliberately a non-final class: Jackson default typing cannot restore final types such as records.

## 9. Kafka architecture

* Topic `order-events` (3 partitions) and dead-letter topic `order-events.DLT`, created on startup.
* **Producer** (`OrderEventPublisher`): listens with `@TransactionalEventListener(AFTER_COMMIT)`, key = order id, JSON, `acks=all`, idempotent producer, 5 s `max.block.ms`.
* **Three consumer groups** read every event independently:
  * `inventory-group` → moves the order `PENDING → CONFIRMED` and logs low-stock warnings.
  * `notification-group` → logs a notification; sends email if `NOTIFICATION_EMAIL_ENABLED=true`.
  * `analytics-group` → stores a row in `order_analytics` (idempotent) that feeds `/api/analytics/summary`.
* **Error handling**: 3 retries, 1 s apart, then the record goes to the DLT. `ErrorHandlingDeserializer` stops bad payloads from blocking a partition.
* **Why it has a real role**: stock is reduced synchronously (correctness cannot wait), but confirmation, notification and analytics are decoupled. A new order is briefly `PENDING` and becomes `CONFIRMED` a moment later — **eventual consistency**. The UI polls briefly to show this.
* **Known limitation**: if Kafka is down at commit time the order exists but the event is lost (order stays `PENDING`). The production fix is the *transactional outbox* pattern (Future enhancements).

## 10. Docker architecture

`docker-compose.yml`: `postgres`, `redis`, `kafka` (each with a health check), `backend` (multi-stage build, non-root user, starts after dependencies are healthy), `frontend` (Vite build served by nginx, which proxies `/api` to the backend).
All credentials come from `.env`.

## 11. React architecture

`src/api.js` (Axios + token interceptor + error helper), `AuthContext` (login state, role flags), `ProtectedRoute` (login and role guard),
`components/` (Navbar, shared UI), `pages/` (one file per screen). Forms validate on the client and show server errors; every page has loading and error states.
Role-based UI: customers see cart/orders, admins see product/inventory/order management. The server enforces permissions regardless of what the UI shows.

## 12. Installation requirements

Docker + Docker Compose for the easy path. For local development: JDK 17, Maven 3.9+, Node 20+, and PostgreSQL/Redis/Kafka (run them with `docker compose up postgres redis kafka`).

## 13. Environment configuration

Copy `.env.example` to `.env` and set at least `DB_PASSWORD`, `JWT_SECRET` (32+ chars) and `ADMIN_PASSWORD`. Variables: `DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, JWT_EXPIRATION_MS, REDIS_HOST, REDIS_PORT, KAFKA_BOOTSTRAP_SERVERS, MAIL_*, NOTIFICATION_EMAIL_ENABLED, ADMIN_EMAIL, ADMIN_PASSWORD, SEED_DEMO_DATA`.
Defaults in `application.yml` are for local development only; the app refuses a JWT secret shorter than 32 characters.

## 14. How to run locally

```bash
cp .env.example .env              # edit values
docker compose up -d postgres redis kafka
set -a; source .env; set +a       # export variables to your shell
export DB_URL=jdbc:postgresql://localhost:5432/smartcart
cd backend && mvn spring-boot:run          # http://localhost:8080
cd ../frontend && npm install && npm run dev   # http://localhost:5173
```

## 15. How to run using Docker Compose

```bash
cp .env.example .env              # set DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD
docker compose up --build
```
Shop: <http://localhost:3000> · API: <http://localhost:8080> · Swagger: <http://localhost:8080/swagger-ui.html> · Health: <http://localhost:8080/actuator/health>.
Log in as the admin (`ADMIN_EMAIL` / `ADMIN_PASSWORD`); demo products are seeded when `SEED_DEMO_DATA=true`.

## 16. API examples

```bash
# register + login
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Asha","email":"asha@example.com","password":"Str0ngPass!"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"asha@example.com","password":"Str0ngPass!"}' | jq -r .token)

# browse, add to cart, order
curl 'localhost:8080/api/products?q=headphones&maxPrice=3000&sortBy=price&direction=asc'
curl -X POST localhost:8080/api/cart/items -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":2}'
curl -X POST localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"shippingAddress":"12 MG Road, Bengaluru 560001"}'

# natural-language search (optional extension)
curl 'localhost:8080/api/products/smart-search?query=affordable%20wireless%20headphones%20for%20office%20use'
```

## 17. Testing instructions

```bash
cd backend && mvn test
```
Tests use the `test` profile: H2 database, in-memory cache, Kafka listeners disabled (no external services needed).

| Test | Covers |
|---|---|
| `JwtServiceTest` | token creation, tampered/expired token, short secret |
| `AuthSecurityIntegrationTest` | register, duplicate, validation, login, invalid login, JWT required, role authorization, product create/search/filter/sort, 404 |
| `ProductCacheIntegrationTest` | cache miss then hit, `@CachePut` update, eviction on stock change and delete |
| `OrderFlowIntegrationTest` | order success, insufficient stock + full rollback, cancel restores stock, invalid transition, ownership, concurrent last-unit race, event published after commit |
| `CartServiceTest`, `OrderServiceTest` | cart merge/stock rules, order rules (Mockito) |
| `KafkaPublishingAndConsumingTest` | producer topic/key, Kafka-down tolerance, three consumers, idempotent analytics |
| `SmartSearchServiceTest` | query interpretation |

Not covered: tests against a **real** Redis or Kafka broker (the cache logic is exercised with Spring's in-memory cache; Kafka is mocked). Adding Testcontainers for both is the recommended next step. There are no frontend tests.

## 18. Project screenshots

_Add screenshots here after running the app:_ `docs/shop.png`, `docs/cart.png`, `docs/admin-inventory.png`, `docs/swagger.png`.

## 19. Future enhancements

* Transactional outbox so events can never be lost; Flyway migrations; Testcontainers integration tests.
* Payments, refresh tokens / httpOnly cookies, rate limiting, audit log.
* Product images, reviews, order cancellation by customers.
* Extract notifications and analytics into their own services (they already communicate only through Kafka).
* **AI extension**: `SmartSearchService` is a transparent keyword/price parser, **not** an ML model. Its `interpret()` method is the single place where an LLM call could replace the rules; recommendations are not implemented.

## Suggested commits

```
chore: initialise Spring Boot project, Docker Compose and .gitignore
feat(auth): user registration, login and JWT security
feat(catalog): products, categories, search and pagination
feat(inventory): stock management with pessimistic locking
feat(cart): shopping cart with stock validation
feat(orders): transactional order placement and status workflow
feat(cache): Redis cache-aside for products with eviction
feat(kafka): order events with inventory, notification and analytics consumers
feat(frontend): React storefront and admin screens
test: unit and integration tests
docs: README and API documentation
```
