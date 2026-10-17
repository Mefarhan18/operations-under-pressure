# Newtonite Operations — "Operations Under Pressure"
## High-Reliability Operations Management Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React 18](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.5-blue.svg)](https://www.typescriptlang.org/)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Automated%20Tests-38%2F38%20Passing-brightgreen.svg)]()

---

## 1. Project Overview & Problem Solved

In high-pressure operational environments (such as payment processing outages, security vulnerability triage, or compliance audits), operational teams struggle with:
1. **Uncoordinated Duplicate Work**: Multiple operators filing duplicate tickets or attempting to resolve the same incident independently.
2. **Silent Overwrites & Race Conditions**: Operators overwriting each other's updates because their screens displayed stale data.
3. **Double-Claiming**: Multiple on-call engineers simultaneously claiming the same urgent incident.
4. **Authorization Leaks**: Cross-team privilege leakage where users perform unauthorized approvals outside their domain.
5. **Lack of Auditability**: Inability to reconstruct the exact timeline of who approved or transitioned a ticket.

**Newtonite Operations** solves these operational failures with a **deterministic, concurrency-safe, append-only management system**. It provides atomic task claiming, optimistic lock conflict detection, idempotent ticket creation, fine-grained multi-tenant team authorization, and strict workflow transitions.

---

## 2. Key Features Actually Implemented

- **JWT Authentication & Role Context**: Signed, stateless JWT authentication with BCrypt password hashing. Development demo users pre-seeded with quick-login chips on the login screen.
- **Multi-Tenant Team Authorization**: Users can belong to multiple teams with distinct roles (`VIEWER`, `MEMBER`, `LEAD`, `ADMIN`). Cross-team access to items, activity logs, or comments is strictly blocked at the backend with HTTP 403 Forbidden.
- **Workflow State Machine**: Strict progression from `OPEN -> IN_PROGRESS -> AWAITING_APPROVAL -> RESOLVED -> CLOSED`. Illegal transitions are rejected with HTTP 409 Conflict.
- **Lead/Admin Approval Gating**: Only users holding `LEAD` or `ADMIN` roles within the work item's specific team can approve items (`AWAITING_APPROVAL -> RESOLVED`).
- **Atomic Task Claiming**: Single-statement conditional JPQL update (`WHERE assignee IS NULL`) prevents race conditions. Multi-threaded claim races result in exactly 1 claim and 1 HTTP 409 Conflict for the loser.
- **Optimistic Locking & Concurrency UX**: Database `@Version` column prevents stale overwrites. The React UI renders a dedicated Concurrency Conflict Banner (HTTP 409) with a one-click **"Reload Latest Data"** action.
- **Creation Idempotency (`POST /api/items`)**: Accepts `Idempotency-Key` UUID header. Backed by MySQL `idempotent_requests` table with SHA-256 payload fingerprinting. Replaying identical keys returns the original response; tampered payloads return HTTP 409 Conflict.
- **Append-Only Activity History**: Every creation, claim, update, priority change, and status transition appends an immutable record to `activity_events`.
- **Discussion Stream**: Team members can add and read comments on work items.
- **Summary Metrics Dashboard**: Database-level count queries (`GET /api/items/summary`) calculate open, urgent, assigned, and pending approval metrics without downloading entire datasets.
- **Search, Filter & Bounded Pagination**: Full-text substring search across title and description, filtering by team, priority, status, and assignee with server-side pagination.
- **N+1 Query Elimination**: Eager `LEFT JOIN FETCH` for `team`, `assignee`, and `createdBy` ensures list queries execute in a single SQL statement.
- **Live System Telemetry**: Health endpoint (`/api/v1/health`) continuously monitors MySQL engine connection, pool statistics, and service status.

---

## 3. Technology Stack

### Backend
- **Language**: Java 21 LTS
- **Framework**: Spring Boot 3.3.4
- **Security**: Spring Security 6 with JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson` 0.12.6)
- **Persistence**: Spring Data JPA / Hibernate 6.5
- **Database**: MySQL 8.0 (with in-memory H2 MySQL mode for isolated tests)
- **Migrations**: Flyway (`V1__init_schema.sql`, `V2__idempotent_requests.sql`)
- **Validation**: Jakarta Bean Validation (`hibernate-validator`)
- **Testing**: JUnit 5, MockMvc, AssertJ, Spring Boot Test

### Frontend
- **Framework**: React 18
- **Build Tool**: Vite 5
- **Language**: TypeScript 5.5
- **Routing**: React Router v6
- **Server State**: TanStack Query v5 (React Query)
- **HTTP Client**: Axios with centralized request/response interceptors
- **Styling**: Tailwind CSS
- **Icons**: Lucide React

---

## 4. Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                   React 18 + TypeScript                     │
│  - Vite Dev Server / Static Dist Engine                      │
│  - Centralized Axios Client (Bearer Token Interceptor)       │
│  - TanStack Query (Targeted Cache Invalidation & Polling)   │
│  - Role-Aware Dynamic UI (Lead Approval Gate & 409 Banner)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP / JSON REST
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot 3.3.4 (Java 21)                 │
│  - JwtAuthenticationFilter (Stateless Bearer Validation)    │
│  - TeamAuthorizationService (Role & Team Isolation Checks)  │
│  - WorkItemService (Idempotency, State Machine, Audit Log)  │
│  - GlobalExceptionHandler (Clean 400/401/403/404/409/500)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC (HikariCP)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                          MySQL 8.0                          │
│  - Schema managed by Flyway (V1 init, V2 idempotency)       │
│  - Tables: users, teams, memberships, work_items,           │
│    activity_events, comments, idempotent_requests           │
│  - Optimistic locking version column & unique constraints   │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Prerequisites

- **Java Development Kit**: JDK 21 LTS (`java -version` returns 21+)
- **Build Tool**: Apache Maven 3.9+ (`mvn -version`)
- **Node.js**: v18.0.0 or higher (`node -v`) & npm (`npm -v`)
- **Database**: MySQL 8.0 (running locally on port 3306 or via Docker)

---

## 6. Environment Variable Configuration

The project uses `.env` in the repository root for configuration:

```bash
cp .env.example .env
```

| Variable | Description | Default |
| :--- | :--- | :--- |
| `DB_HOST` | MySQL host | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | MySQL database name | `devpilot` (or `newtonite_ops`) |
| `DB_USERNAME` | MySQL username | `devpilot_user` |
| `DB_PASSWORD` | MySQL password | `devpilot_pass` |
| `SERVER_PORT` | Backend HTTP port | `8080` |
| `FRONTEND_PORT` | Frontend HTTP port | `5173` |
| `VITE_API_URL` | Frontend API target | `http://localhost:8080` |
| `CORS_ALLOWED_ORIGINS` | Permitted browser origins | `http://localhost:5173,http://localhost:3000` |
| `SEED_DATA_ENABLED` | Pre-seed demo users & teams | `true` |
| `JWT_SECRET` | 256-bit JWT signing secret | Pre-configured secure fallback |

---

## 7. Starting the Application

### Option A: Local Processes (Recommended for Development)

#### 1. Ensure MySQL is running
Ensure MySQL 8.0 is running on port 3306 with the database and credentials configured in `.env`.

#### 2. Start Backend
In terminal 1:
```bash
cd backend
mvn spring-boot:run
```
*The backend initializes Flyway schema migrations (`V1`, `V2`), runs the data seeder if the database is empty, and listens on `http://localhost:8080`.*

#### 3. Start Frontend
In terminal 2:
```bash
cd frontend
npm install
npm run dev
```
*The React frontend will be accessible at `http://localhost:5173`.*

---

### Option B: Docker Compose (All-in-One Containerized)

To start MySQL, Backend, and Frontend together:
```bash
docker-compose up --build
```
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- MySQL: `localhost:3306`

---

## 8. Database Migrations & Schema Initialization

Schema creation is fully automated via **Flyway**:
- `V1__init_schema.sql`: Creates core tables (`users`, `teams`, `memberships`, `work_items`, `activity_events`, `comments`), foreign keys, and indexes.
- `V2__idempotent_requests.sql`: Creates the `idempotent_requests` table with composite unique constraint `uk_idempotency_key_user` on `(user_id, idempotency_key)` and lookup indexes.

Flyway runs automatically on application startup. To trigger migrations manually:
```bash
cd backend
mvn flyway:migrate
```

---

## 9. Demo Accounts (BCrypt Hashed)

All demo accounts are seeded with BCrypt-hashed passwords (`Password123!`). No plaintext passwords exist in the database:

| Email | Role & Team Membership | Operational Privileges |
| :--- | :--- | :--- |
| `admin@newtonite.internal` | `ADMIN` (*Core Platform Operations*) | Full administrative privileges, user management, and approval authority |
| `lead@newtonite.internal` | `LEAD` (*Payment & Financial Clearing*) | Full operational rights + **Approval authority** (`AWAITING_APPROVAL -> RESOLVED`) |
| `ops@newtonite.internal` | `MEMBER` (*Core Platform Operations*) | Create, claim, update, comment, and standard workflow transitions |
| `auditor@newtonite.internal` | `VIEWER` (*Payment*), `LEAD` (*Compliance*) | Read-only auditor in Payment team; full Lead approval in Compliance team |

*Tip: The Login view at `/login` provides single-click demo credential chips to switch roles quickly.*

---

## 10. How to Run Automated Tests

### Backend Test Suite (38/38 Passing)
Run all integration, concurrency, authorization, and transactional tests:
```bash
cd backend
mvn test
```

Expected output:
```
[INFO] Results:
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Key test suites:
- `IdempotencyIntegrationTest`: Tests duplicate retry replay, 409 on modified payload, multithreaded race safety, and 24h retention purge.
- `ConcurrentClaimingIntegrationTest`: Multi-threaded race using 2 threads and `CountDownLatch`; verifies exactly 1 claim succeeds, 1 receives 409 Conflict, and exactly 1 activity event is logged.
- `StaleVersionRejectionTest`: Verifies 409 Conflict on stale PATCH and transition requests, plus simultaneous concurrent updates race.
- `AuthorizationAndCrossTeamAccessTest`: Validates 403 Forbidden for Viewer modifications, Member approval attempts, cross-team Lead approvals, and cross-team activity/comment access.
- `TransactionalIntegrityTest`: Proves atomic commits of item + activity event and atomic rollbacks upon failure.
- `WorkflowTransitionsTest`: Verifies full lifecycle progression and rejection of illegal state jumps.

### Frontend Production Build & Type Check
Verify TypeScript compilation and Vite production bundling:
```bash
cd frontend
npm run build
```
Expected output:
```
> tsc && vite build
✓ 1680 modules transformed.
dist/index.html                   0.67 kB
dist/assets/index-JubnJy3p.css   30.64 kB
dist/assets/index-Byhd0GBg.js   344.34 kB
✓ built in 2.30s
```

---

## 11. Main REST API Endpoints

All endpoints except `/api/auth/login` require an `Authorization: Bearer <token>` header:

| Method | Endpoint | Description | Permitted Roles |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate and obtain signed JWT | Public |
| `GET` | `/api/me` | Fetch authenticated user profile & team memberships | Authenticated |
| `GET` | `/api/items` | Filtered, searchable, paginated work items | Team Members |
| `GET` | `/api/items/summary` | Dashboard metric counts (open, urgent, assigned, approval) | Team Members |
| `POST` | `/api/items` | Create work item (accepts `Idempotency-Key` header) | `MEMBER`, `LEAD`, `ADMIN` |
| `GET` | `/api/items/{id}` | Fetch full work item details | Team Members |
| `PATCH`| `/api/items/{id}` | Update item fields (requires matching `version`) | `MEMBER`, `LEAD`, `ADMIN` |
| `POST` | `/api/items/{id}/claim` | Atomically claim unassigned work item | `MEMBER`, `LEAD`, `ADMIN` |
| `POST` | `/api/items/{id}/transition` | Advance status (`OPEN -> IN_PROGRESS -> AWAITING_APPROVAL -> RESOLVED -> CLOSED`) | Member / Lead for Approval |
| `GET` | `/api/items/{id}/activity`| Retrieve chronological append-only audit trail | Team Members |
| `GET` | `/api/items/{id}/comments`| Retrieve discussion comments | Team Members |
| `POST` | `/api/items/{id}/comments`| Post discussion comment | `MEMBER`, `LEAD`, `ADMIN` |
| `GET` | `/api/v1/health` | Live MySQL connection & telemetry monitor | Public / Monitoring |

---

## 12. Known Limitations

1. **Synchronous Notifications**: Live updates rely on TanStack Query targeted background polling (10s on health telemetry, 30s on summary metrics) rather than WebSockets. This was a deliberate architectural decision to ensure read-your-own-writes consistency without broker operational overhead.
2. **File Attachments**: Work items support rich descriptions and comments, but binary file uploads (e.g. log attachments or PDF screenshots) are not implemented in the current scope.
3. **Idempotency Key Scope**: Idempotency is enforced on `POST /api/items`. Other mutations rely on optimistic locking (`version` matching) and atomic conditional updates for duplicate and concurrency protection.

---

## 13. 3–5 Minute Evaluation Demo Script

1. **Login & Role Awareness**:
   - Navigate to `http://localhost:5173/login`. Click the **Operator** chip (`ops@newtonite.internal`).
   - Observe immediate redirect to `/dashboard` showing real-time summary cards and personal queues.
2. **Search & Filter**:
   - Click **Work Items** in the sidebar. Filter by Priority: `URGENT` or search for `"Database"`. Observe instant server-side pagination.
3. **Create with Idempotency**:
   - Click **+ Create Work Item**. Fill in an urgent incident title, select team *Core Operations*, and submit.
   - The ticket is created with optimistic lock `Version: 0`.
4. **Atomic Claim & Progression**:
   - Open the created work item. Click **"Claim Work Item"**. Observe immediate assignment to yourself and the recorded `CLAIMED` event in the audit trail.
   - Click **"Begin Work"** (`OPEN -> IN_PROGRESS`), then **"Submit for Approval"** (`IN_PROGRESS -> AWAITING_APPROVAL`).
5. **Role-Gated Approval (Security Enforcement)**:
   - As an Operator (`MEMBER`), observe the locked badge: *"Approval requires Lead or Admin authority"*.
   - Log out and log in as **Lead** (`lead@newtonite.internal`). Navigate to the work item.
   - Click **"Approve & Resolve"** (`AWAITING_APPROVAL -> RESOLVED`).
6. **Concurrency Conflict Demonstration**:
   - Open the item in two browser windows. In Window 1, update priority to `LOW`.
   - In Window 2 (holding stale version), click **"Close Work Item"**.
   - Observe the dedicated **HTTP 409 Concurrency Conflict Banner** prompting to reload latest data.
7. **Audit Trail Verification**:
   - Scroll to the **Activity History** section to view the immutable append-only event trail showing every transition actor, delta, and timestamp.
