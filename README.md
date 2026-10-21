# Document Management System (DMS): Technical Report

Enterprise Software Project: Front-end, Microservices Backend, Security and DevOps

*Team report. Replace the names in Section 11 with your team members.*

## 1. Executive Summary

This project delivers a modern Document Management System (DMS) that lets users upload, organize, search, share and collaborate on documents. The work covers three layers:

- **Front-end**: an admin and user dashboard (user management, folders, uploads with metadata, search, pagination, filters, favorites).
- **Backend**: a microservices architecture (authentication, documents, search, notifications, workflow) behind an API Gateway.
- **Cross-cutting concerns**: JWT security with optional Keycloak delegation, S3-compatible storage (MinIO), automated testing, CI/CD, reactive programming and an analytics dashboard.

The central design principle is **separation of responsibilities**: identity is handled in one place, documents in another, and every request is authenticated once at the gateway and authorized where the data lives.

## 2. Functional Requirements

| Area | Requirement |
| --- | --- |
| Document handling | Upload, download, versioning, metadata for multiple file types |
| User management | Authentication, authorization, role-based access control (RBAC) |
| Search | Full-text search and advanced filtering |
| Notifications | Alerts on document changes, mentions and workflow updates |
| Workflow | Approval flows, reviews, task assignments |
| Front-end | Admin creates and manages users; normal users view and create documents; folders; search, pagination, filters, favorites |
| Business rule | Users see and download only documents of the departments assigned to them |

## 3. Front-end Design

### 3.1 Approach

The front-end was built on a professional dashboard template with a CSS framework, reusing existing components, and integrated against a dummy backend (FakeAPI / JSON mock) until the real services were available. Because the API contract was fixed early, switching from the mock to the real gateway only required changing the base URL.

### 3.2 Implemented features

1. **User management (admin only)**: create, edit, deactivate users; assign one of two roles (admin, normal) and departments.
2. **Folders**: create, rename, nest and navigate folders.
3. **Document upload with metadata**: title, description, tags, department, document type, author, date.
4. **Normal user capabilities**: view and create documents; no access to the user administration screens.
5. **Search, pagination and simple filters**: keyword search, filter by type, department, date and owner; server-side pagination.
6. **Favorites**: star or unstar documents, with a dedicated favorites view.

### 3.3 Role-based UI

The JWT returned at login contains the user's roles. The front-end reads them to show or hide menu entries (for example *User Management* only for admins) and guards routes. This is a usability layer only: the real enforcement happens in the backend, so a user who edits the UI still cannot obtain data they are not allowed to see.

## 4. Backend Architecture

### 4.1 Microservices identified

The requirements map to five business services plus infrastructure services.

| Microservice | Role and functionality |
| --- | --- |
| **Auth / Identity Service** | Registers users, verifies credentials, issues JWT tokens, manages roles, tracks login statistics |
| **Document Service** | Upload, download, versioning, metadata, folders, favorites, sharing permissions, department-based access |
| **Search Service** | Indexes content and metadata, serves full-text queries and advanced filters |
| **Notification Service** | Consumes events and alerts users (changes, mentions, workflow updates) by in-app, email or push |
| **Workflow Service** | Approval flows, reviews, task assignments, state transitions |
| **API Gateway** | Single entry point, routing, JWT validation, header propagation, circuit breaking |
| **Analytics Service** (bonus) | Aggregates usage events for the dashboard |

### 4.2 Data management per service

| Service | Data handled | Database type | Why |
| --- | --- | --- | --- |
| Auth | Emails, password hashes, salts, roles, last login, last logout, login count | Relational (H2 in dev, PostgreSQL in production) | Strong consistency and transactions on credentials |
| Document | Document metadata, versions, folders, permissions, favorites; binary files in object storage | Relational (PostgreSQL) for metadata, **S3 / MinIO** for files | Structured relations plus cheap, scalable binary storage |
| Search | Inverted index of content and metadata | Search engine (Elasticsearch or OpenSearch) | Optimized for full-text and faceted queries |
| Notification | Notifications, read status, preferences | Document-oriented (MongoDB) | Flexible payloads, high write volume |
| Workflow | Workflow definitions, instances, tasks, history | Relational (PostgreSQL) | Transactional state machines and audit trail |
| Analytics | Usage events | Time-series or columnar store | Aggregation-heavy reads |

A graph database (Neo4j) could be added later to model complex sharing relationships (user, group, document), but it is not needed for the baseline.

### 4.3 Data redundancy and consistency

**Controlled redundancy.** Each service owns its data (database-per-service). Some duplication is deliberate:

- The Search Service stores a copy of document metadata and extracted text.
- Other services store only the **user ID** (and optionally a display name), never the full user record.
- The Notification Service keeps a copy of the document title for message rendering.

**Consistency strategy.**

- **Within a service**: ACID transactions.
- **Across services**: **eventual consistency** through asynchronous events (for example *DocumentCreated*, *DocumentUpdated*, *WorkflowApproved*) published on a message broker (Kafka or RabbitMQ).
- Multi-step business processes such as an approval flow use the **Saga pattern**, with compensating actions on failure.
- Consumers are idempotent, so a replayed event does not corrupt state.
- The Document Service remains the **source of truth**. If the index drifts, it can be rebuilt from the Document Service.

### 4.4 Technology stack and ownership

| Service | Stack | Owner |
| --- | --- | --- |
| Auth Service | **Java Spring Boot**, Spring Security, JJWT, H2 then PostgreSQL | Student 1 |
| API Gateway | Spring Cloud Gateway (WebFlux, reactive) or Nginx, Resilience4j | Student 2 |
| Document Service | Spring Boot (WebFlux), PostgreSQL, MinIO S3 client | Student 3 |
| Search Service | Spring Boot plus Elasticsearch | Student 4 |
| Notification Service | Spring Boot or Node.js, MongoDB, RabbitMQ | Student 5 |
| Workflow Service | Spring Boot plus a workflow engine (Camunda or Flowable) or custom state machine | Student 6 |
| Front-end | Dashboard template plus CSS framework, consuming the gateway API | Whole team |

Java Spring Boot is used for the services, which satisfies the requirement that at least one microservice use it.

## 5. Security and Access Control

### 5.1 Where credentials are stored and verified

Credentials live **only in the Auth Service**. Passwords are never stored in clear text: each user has a **random salt** and a **hash** (BCrypt). At login the service loads the user, hashes the submitted password with the stored salt, compares, then issues a signed JWT. No other service ever sees a password.

### 5.2 How the Document Service knows the user's role

The JWT carries the roles claim (for example `ROLE_ADMIN`, `ROLE_USER`). The gateway validates the token and forwards the result in trusted headers (`X-User-Name`, `X-User-Roles`, `X-User-Departments`). The Document Service reads these headers; it does not call the Auth Service on each request.

### 5.3 How the Document Service decides which documents a user can view

The rule is **department-based filtering**: *a user sees only documents of the departments assigned to them*. The departments arrive as a token claim. The Document Service applies them as a mandatory filter in the database query (`WHERE department IN (:userDepartments)`) and re-checks on download. Admins may be granted wider scope. Fine-grained sharing adds a second path: a document explicitly shared with the user (VIEW, COMMENT, EDIT) is also visible. Filtering in the query, rather than after fetching, keeps pagination and search counts correct and prevents leaks.

### 5.4 What the Auth Service must pass to the Document Service

- User ID (subject) and username
- Roles
- Departments
- Token issue and expiry times (validated at the gateway)

### 5.5 Is an API Gateway needed? Yes

- **Single entry point**: clients know one URL; internal services stay private.
- **Centralized authentication**: the JWT is validated once, not in every service.
- **Header propagation**: identity is passed downstream in a consistent form.
- **Cross-cutting features**: routing, rate limiting, CORS, logging, circuit breaking.
- **Resilience**: Resilience4j circuit breakers prevent cascading failures.

The trade-off is an extra network hop and a potential single point of failure, mitigated by running multiple gateway instances. Because downstream services trust the gateway headers, they must not be reachable directly from outside the network (defense in depth: services can also re-validate the JWT).

### 5.6 Keycloak option

The system supports two interchangeable identity modes: (1) the **custom Auth Service** with a database, or (2) delegation to **Keycloak** as an OpenID Connect identity server. In the Keycloak mode, the gateway validates tokens against Keycloak's public keys (JWKS), and roles and departments come from Keycloak realm roles and custom claims. Downstream services see the same headers in both modes, so they are unaffected by the choice.

## 6. Exercise Walkthrough: Building the Authentication Service

### C2: Minimal Auth Service (hardcoded users)

1. Created a Spring Boot project (Gradle) with `spring-boot-starter-web`, `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson` and Lombok.
2. Defined a **User model** (username, password, roles) with no database.
3. Wrote a **UserService** with two hardcoded users: `admin` with `ROLE_ADMIN` and `ROLE_USER`, and `user` with `ROLE_USER` only.
4. Implemented **JWT utilities** to generate, parse and validate tokens.
5. Externalized the **secret and expiration** in `application.yml`.
6. Exposed `POST /auth/login` through a REST controller.
7. Verified token generation with **Postman**.

### C3: Persistence with H2

The service was extended with an H2 table storing email, password hash, **salt**, last login date, last logout date, login count and roles. Each successful login updates the last login date and increments the counter, and logout updates the last logout date. This gives basic auditing and feeds the analytics dashboard.

### C4: API Gateway

A Spring Cloud Gateway application (WebFlux, circuit breaker with Resilience4j, JJWT) was started with:

1. A route to the auth-service and a prepared route for a future hello-service.
2. The same JWT secret as the auth-service.
3. A **token validation filter** that requires the `Authorization` header, validates the JWT and extracts user and roles.
4. **Header propagation** of username and roles to downstream services.

## 7. Advanced Features

| Feature | Implementation |
| --- | --- |
| **S3 storage** | Files stored in MinIO (Docker); the database keeps only the object key and metadata; pre-signed URLs or streamed downloads |
| **Fine-grained sharing** | Permission model per document: VIEW, COMMENT, EDIT, SHARE for a user, group or department |
| **Reactive programming** | WebFlux and Reactor in the gateway and document service for non-blocking file streaming and high concurrency |
| **Analytics dashboard** | Events (logins, uploads, downloads, searches) aggregated into charts: active users, top documents, storage use |
| **Documentation** | OpenAPI/Swagger for every API |
| **Testing** | Unit, integration and end-to-end (see Section 8) |
| **CI/CD** | Automated pipeline (see Section 9) |

## 8. Testing Strategy

- **Unit tests** (JUnit 5, Mockito): JWT utility, role mapping, department filter logic.
- **Integration tests** (Spring Boot Test, Testcontainers with PostgreSQL and MinIO): repository queries, upload and download, login endpoint.
- **Contract tests**: the API contract between front-end and services.
- **End-to-end tests** (Cypress or Playwright): login, create folder, upload, search, favorite, plus a negative test confirming a user cannot see another department's document.

## 9. CI/CD Pipeline

On every push or pull request, the pipeline builds, runs unit tests, runs integration tests, performs static analysis, builds Docker images and pushes them to a registry. On merge to the main branch it deploys to a staging environment with Docker Compose (or Kubernetes), runs end-to-end tests, then promotes to production. A failure at any stage stops the pipeline, so only tested code is deployed.

## 10. Technical Strengths

1. **Clear service boundaries**: each service has a single responsibility and its own data store, which allows independent development, scaling and deployment.
2. **Right database for each job**: relational for transactions, search engine for full-text, document store for notifications, object storage for files.
3. **Security by design**: salted password hashes, signed JWTs with expiry, a central gateway check, department-level data filtering enforced in the query, and secrets externalized in configuration.
4. **Identity flexibility**: a custom Auth Service and Keycloak are interchangeable without changing downstream services.
5. **Resilience and scalability**: circuit breakers, stateless services (JWT, no sessions), reactive non-blocking I/O and event-driven communication.
6. **Loose coupling**: asynchronous events let services evolve and fail independently, with eventual consistency managed through sagas and idempotent consumers.
7. **Quality assurance**: layered testing and automated CI/CD reduce regressions.
8. **Maintainability**: documented APIs, containerized environments and a front-end decoupled through a stable API contract (with a mock backend for parallel development).
9. **Auditability**: login and logout tracking, workflow history and document versioning.

## 11. Team Contributions

| Member | Responsibility |
| --- | --- |
| Student 1 | Auth Service (Spring Boot, JWT, H2) |
| Student 2 | API Gateway |
| Student 3 | Document Service and MinIO integration |
| Student 4 | Search Service |
| Student 5 | Notification Service |
| Student 6 | Workflow Service |
| All | Front-end, testing, documentation, CI/CD |

## 12. Conclusion

The project shows how a DMS can be decomposed into cohesive microservices with secure, centralized authentication, department-aware authorization and a scalable storage and search layer. The architecture satisfies the functional requirements while remaining extensible toward sharing, analytics, Keycloak and fully automated delivery.

## 13. Diagrams

The diagrams follow below: system architecture, login and JWT flow, document access decision, data model and the CI/CD pipeline.

### Figure 1: System architecture

&#91;embedded content: system architecture · gateway, 5 services, broker\]

Clients talk only to the gateway. Each service keeps its own store, and the broker carries events between them.

### Figure 2: Login and JWT validation

&#91;embedded content: login and request flow · 4 actors, 10 steps\]

The Auth Service verifies the password once and signs a JWT. After that, the gateway validates the token and passes the user's identity downstream; the Document Service never sees a password.

### Figure 3: Document access decision

&#91;embedded content: document access decision · 3 checks, 3 outcomes\]

The same rule gates viewing and downloading. Filtering inside the database query, rather than after the fetch, keeps pagination and search counts correct.

### Figure 4: Data model

&#91;embedded content: data model · 2 databases, 7 tables\]

Credentials stay in the Auth database. The Document database stores metadata and the storage key of each file version; the binary itself lives in MinIO.

### Figure 5: CI/CD pipeline

&#91;embedded content: CI/CD pipeline · 8 stages\]

Every commit is built and tested. Only merges to the main branch are packaged, deployed to staging, checked end to end and promoted.
