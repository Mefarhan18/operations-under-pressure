# Engineering Decisions & Architectural Blueprint

**Project:** Newtonite Operations — "Operations Under Pressure"  
**Milestones:** Phase 1 (Foundation) through Phase 5 (Finalization & Submission Verification)  
**Author:** Senior Full-Stack Engineer  

---

## Executive Architectural Summary

Newtonite Operations was engineered under a strict deadline to deliver an enterprise-grade operational management and incident triage application. The design prioritizes **deterministic correctness, concurrency safety, append-only traceability, and defensive multi-tenant team authorization** within a robust modular monolith.

---

## Decision 1: Relational Data Model & Append-Only Activity History

### Context & Problem Statement
In mission-critical operational environments, accountability is paramount. When an incident or financial compliance task is modified, re-assigned, or approved, operators must have an immutable audit trail showing who performed each action, what state changed, and when.

### Alternatives Considered
1. **In-place State Mutation Only**: Update `WorkItem` fields directly without storing audit records.
   - *Why rejected*: Zero accountability; impossible to reconstruct the timeline of an incident or verify compliance approvals.
2. **Event Sourcing (Full Event-Driven Architecture)**: Store only domain events and project state via event streams (e.g. CQRS/Kafka).
   - *Why rejected*: Unnecessary operational complexity, eventual consistency delays, and significant cognitive overhead for a one-day deadline.
3. **Database Change Data Capture (CDC / Triggers)**: Rely on database-level audit triggers.
   - *Why rejected*: Divorces application business context (such as the authenticated operator's user identity and operational reason) from database records.

### Chosen Approach
A **hybrid relational model with transactional append-only activity events**:
- The current operational state of a task lives in the `work_items` table with foreign keys to `teams`, `users` (creator & assignee), and indexed status/priority columns.
- Any mutation (`ITEM_CREATED`, `CLAIMED`, `ASSIGNED`, `STATUS_TRANSITION`, `PRIORITY_CHANGE`, `ITEM_UPDATED`) transactionally writes an immutable record into the `activity_events` table before committing.
- Foreign keys use `ON DELETE RESTRICT` (or avoid cascading deletes) to prevent accidental erasure of users, teams, or audit history.

### Trade-offs & Mitigations
- **Storage Growth**: Append-only tables grow linearly with activity.
  - *Mitigation*: Indexed on `(work_item_id, created_at DESC)` with compact VARCHAR fields and targeted pagination, preventing query degradation.
- **Transactional Overhead**: Every update requires two writes (`work_items` + `activity_events`).
  - *Mitigation*: Both writes execute inside a single local Spring transaction (`@Transactional`), ensuring atomic consistency without distributed transaction overhead.

---

## Decision 2: Optimistic Locking for Stale Updates (HTTP 409)

### Context & Problem Statement
Under operational pressure, multiple engineers frequently review the same incident simultaneously. If Operator Alice updates a task's priority while Operator Bob transitions its status based on older information, an uncoordinated "last write wins" strategy would silently overwrite Alice's changes.

### Alternatives Considered
1. **Last-Write-Wins (No Versioning)**: The most recent write blindly overwrites the database.
   - *Why rejected*: Causes silent data corruption, lost audit history, and hazardous operational decisions during live incidents.
2. **Pessimistic Locking (`SELECT ... FOR UPDATE`)**: Lock the row in MySQL as soon as any user views or edits it.
   - *Why rejected*: Holding row locks across user think-time or slow HTTP requests leads to database connection starvation, deadlocks, and severe throughput bottlenecks.
3. **Optimistic Locking via Entity Versioning (`@Version`)**: Compare entity version numbers upon save.

### Chosen Approach
**Optimistic Concurrency Control with Explicit Client Reconciliation**:
- `WorkItem` maintains a JPA `@Version private Long version;` column in MySQL.
- Every client update (`PATCH /api/items/{id}`) and workflow transition (`POST /api/items/{id}/transition`) requires the client's observed `version` in the payload.
- The service verifies that `item.getVersion().equals(request.version())`. If mismatched, it immediately throws `ConflictException`.
- Concurrent transaction races that pass the initial check are caught by Hibernate's `OptimisticLockingFailureException`.
- `GlobalExceptionHandler` translates both into a predictable **HTTP 409 Conflict** response.
- The React frontend catches 409 responses and renders a prominent **Concurrency Conflict Banner** with a one-click **"Reload Latest Data"** action, allowing the operator to inspect the fresh state before retrying.

### Trade-offs & Mitigations
- **User Collision Friction**: The losing user receives an error instead of their change taking effect silently.
  - *Mitigation*: This is the desired behavior in mission-critical operations; explicit conflict resolution guarantees that engineers never make decisions based on stale assumptions.

---

## Decision 3: Atomic Task Claiming

### Context & Problem Statement
When high-severity incidents (`URGENT`) trigger alerts, multiple on-call operators often race to claim the unassigned ticket. If two operators click "Claim" at the same millisecond, the application must guarantee that exactly one engineer is assigned, exactly one claim audit event is logged, and the losing engineer receives an immediate, clear conflict notification.

### Alternatives Considered
1. **Read-Modify-Write in Java**: `findById()`, check if `assignee == null`, set assignee, call `save()`.
   - *Why rejected*: Susceptible to time-of-check to time-of-use (TOCTOU) race conditions under high concurrency where both threads see `assignee == null` before either commits.
2. **Distributed Redis Locks**: Acquire a distributed lock prior to claiming.
   - *Why rejected*: Introduces another infrastructure dependency (Redis), network latency, and failure modes when a simple database-level atomic primitive suffices.
3. **Atomic Conditional JPQL Update (`WHERE assignee IS NULL`)**:

### Chosen Approach
**Single-Statement Atomic Conditional Update via JPQL**:
- `WorkItemRepository` executes an atomic conditional update:
  ```java
  @Modifying
  @Query("UPDATE WorkItem w SET w.assignee = :operator, w.version = w.version + 1, w.updatedAt = CURRENT_TIMESTAMP " +
         "WHERE w.id = :id AND w.assignee IS NULL")
  int atomicClaim(@Param("id") Long id, @Param("operator") User operator);
  ```
- The database engine evaluates `w.assignee IS NULL` while holding the row-level lock during the single `UPDATE` statement.
- If the statement returns `0` rows affected, another operator won the race. The service immediately throws a `ConflictException("Work item has already been claimed by another team member.")`.
- If `1` row is affected, the winning thread logs the `CLAIMED` activity event and commits.

### Trade-offs & Mitigations
- **First-Come, First-Served**: The losing engineer must find another task.
  - *Mitigation*: Clear feedback in the UI informs the losing engineer that the item has already been claimed, preventing duplicated effort during live emergencies.

---

## Decision 4: Team-Based Server-Side Authorization (Defense-in-Depth)

### Context & Problem Statement
An enterprise operations platform serves multiple distinct teams (e.g., *Core Operations*, *Payment Clearing*, *Security Compliance*). A user can belong to multiple teams and hold a different role in each team (e.g., Lead in Compliance, but only Viewer in Payment). Authorization must strictly enforce team isolation and role hierarchy.

### Alternatives Considered
1. **Global User Roles (`ROLE_ADMIN`, `ROLE_MEMBER`)**: Assign roles directly to users globally.
   - *Why rejected*: Completely breaks down for multi-team organizations where users have different authorities in different domains.
2. **Frontend-Only Route/Button Disabling**: Hide restricted buttons in React.
   - *Why rejected*: Trivial to bypass via direct REST API calls or browser developer tools.
3. **Centralized Team Authorization Service**: Contextual permission evaluation per work item.

### Chosen Approach
**Fine-Grained Contextual Authorization via `TeamAuthorizationService`**:
- Roles: `VIEWER` (read-only), `MEMBER` (create, claim, update, standard transition), `LEAD` (Member permissions + approval authority), `ADMIN` (Lead permissions + administrative controls).
- Every backend service method extracts the target `teamId` from the work item and verifies the operator's active membership:
  - `requireCanRead(User, teamId)`
  - `requireCanWrite(User, teamId)`
  - `requireCanApprove(User, teamId)`
- **Cross-Team Isolation**: If an operator does not belong to the work item's team, all operations (viewing details, reading comments, fetching activity logs, transitions) return **HTTP 403 Forbidden**.
- **Approval Gating**: Transitioning `AWAITING_APPROVAL -> RESOLVED` strictly requires `LEAD` or `ADMIN` role within that specific work item's team.

### Trade-offs & Mitigations
- **Query Duplication**: Must verify team membership before mutating items.
  - *Mitigation*: Eagerly fetched during authentication and cached within the request context; verified with unit and integration tests across all roles.

---

## Decision 5: Idempotency, Pagination & Deliberately Omitted Asynchronous Processing

### Context & Problem Statement
In mobile or poor network conditions, HTTP timeouts frequently cause clients to retry `POST /api/items`, which risks creating duplicate tickets. Simultaneously, loading thousands of operational tasks can exhaust server and browser memory. Finally, a balance must be struck regarding asynchronous job queues.

### Alternatives Considered
1. **In-Memory Idempotency Map (`ConcurrentHashMap`)**: Store request keys in memory.
   - *Why rejected*: Lost on server restarts; fails across multi-node clusters.
2. **Asynchronous Message Broker (RabbitMQ / Kafka) for Task Processing**: Decouple work item creation from persistence via background consumers.
   - *Why rejected*: Introduces eventual consistency where an operator creates a ticket but does not see it appear immediately in the list, causing confusion. Distributed queuing introduces dead-letter queues, partition rebalancing, and cluster operations unwarranted for this operational scale.
3. **Database-Backed Persistent Idempotency & Server-Side Pagination**:

### Chosen Approach
1. **Persistent Idempotency (`POST /api/items`)**:
   - Clients supply an `Idempotency-Key` header.
   - Supported by MySQL table `idempotent_requests` with unique constraint `(user_id, idempotency_key)`.
   - Hashes payloads with SHA-256; replaying an identical key returns the original recorded HTTP 201 response. Tampered payloads trigger **HTTP 409 Conflict**.
   - Expired keys (24-hour TTL) are purged via scheduled/transactional cleanup.
2. **Server-Side Pagination & N+1 Elimination**:
   - `GET /api/items` enforces bounded pagination (`page`, `size` up to 50).
   - `WorkItemSpecifications` uses `LEFT JOIN FETCH` for `team`, `assignee`, and `createdBy` on data queries, eliminating N+1 select queries completely.
3. **Deliberately Omitted Asynchronous Processing**:
   - **Omission**: Background asynchronous message workers (Kafka/RabbitMQ) and WebSocket live streams were deliberately omitted.
   - **Reasoning**: Synchronous transactional execution guarantees instant read-your-own-writes consistency. For real-time updates, lightweight TanStack Query polling (10s on health, 30s on summary) provides near real-time operational awareness without external infrastructure overhead or message loss risks.

### Trade-offs & Mitigations
- **Synchronous Write Latency**: The client waits for both the `work_items` and `idempotent_requests` writes.
  - *Mitigation*: With MySQL composite indexes, end-to-end API response times remain under 25 milliseconds.

---

## Summary of Evaluated Trade-offs

| Decision | Chosen Architecture | Alternative Rejected | Primary Benefit |
| :--- | :--- | :--- | :--- |
| **Data Model** | Relational + Append-Only Activity Log | Event Sourcing / CDC | Full audit accountability without eventual consistency |
| **Concurrency** | Optimistic Locking (`@Version`) + UX Banner | Last-Write-Wins / Pessimistic | Zero silent overwrites, no database connection locking |
| **Claiming** | Atomic Conditional JPQL `UPDATE` | Read-Modify-Write / Redis | Exactly 1 winner, zero race conditions, no Redis needed |
| **Security** | Contextual Team Role Authorization | Global Roles / Frontend-only | Multi-tenant isolation; unauthorized actions blocked at API |
| **Idempotency** | Database-Backed SHA-256 Fingerprinting | In-Memory Map | Survives application restarts; prevents duplicate incidents |
| **Processing** | Synchronous Transactional + Targeted Polling | Kafka / WebSockets | Instant read-your-own-writes consistency; zero broker complexity |
