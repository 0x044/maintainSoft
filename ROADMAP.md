# MaintainSoft — Engineering Roadmap

> **Source baseline:** commit `6411848`, verified 2026-09-24.
> This update is prepared against that source baseline; re-verify claims after each
> implementation phase.
>
> This is a living plan based on the current source, configuration, tests, and Git
> history. Re-read the source before making changes. The roadmap is not a substitute
> for a product decision or a deployment authorization.

## 1. Executive Summary

MaintainSoft is currently a backend-only Spring Boot learning project intended to
exercise production-style patterns for maintenance management. The persistence model
covers departments, users, machines, technicians, repairs, repair history, and spare
parts. The implemented API now includes master-data, inventory, and repair workflows;
release hardening, complete HTTP/database coverage, and hermetic test execution remain open.

**Current release posture: not production-ready.** The next work should be security
containment, reproducible testing, and authorization—not new feature breadth.

The most urgent facts are:

- A JWT RSA private key is tracked under `src/main/resources` and is packaged into the
  application JAR. It must be treated as compromised and rotated.
- The bootstrap manager uses hard-coded, publicly known credentials.
- Public registration is removed; manager-only supervisor invitations now exist.
- URL-level role rules cover the initial user/status/machine/repair routes, and
  representative HTTP 401/403 coverage is green, but method authorization and the
  complete role matrix remain untested.
- Access and refresh token purposes are separated; hashed refresh-token rotation,
  family replay revocation, logout, and scheduled cleanup are implemented.
- The default full context test still passes against the authorized test PostgreSQL
  database and can mutate it; an opt-in disposable-database profile is available, but
  hermetic default isolation is still open.
- The current source compiles on Java 25 and 336 tests pass (one opt-in isolated-profile
  test is skipped without disposable database variables), but MVC/security and
  concurrency coverage remain incomplete.

## 2. Current Baseline

### Technology

- Spring Boot `4.0.6`
- Java release `25`
- Maven (verified with Maven `3.9.16` and Temurin `25.0.4.1`)
- Spring MVC, Spring Data JPA/Hibernate, PostgreSQL
- Spring Security OAuth2 resource server with hand-issued RSA JWTs
- Flyway, Jasypt, Actuator, Springdoc, Lombok
- Resilience4j rate-limiter dependency is present but unused

### Repository shape

- Package root: `com.maintainsoft`
- Conventional layers: `controller`, `service`, `repository`, `entity`, `dto`,
  `enums`, `exception`, `security`
- No frontend is currently present. Earlier Vaadin/Next.js experiments were removed.
- No Maven wrapper, CI workflow, Docker/deployment descriptor, or `README.md` is
  currently committed; an opt-in test profile now exists under `src/test/resources`.
- `flyway.conf` is present but empty and is not a substitute for a configured
  environment-specific migration setup.

### Verified build and test state

The following checks were run with Java 25 against the authorized test PostgreSQL
instance:

```text
mvn -B -ntp clean verify
Result: BUILD SUCCESS — 336 tests passed, 1 opt-in test skipped
```

Flyway validated and applied V1–V8, Hibernate initialized against PostgreSQL 18.6, and
the application context started successfully. A test-skipping package build also
succeeds. The passing suite includes the full Spring context test, Mockito unit tests,
accessor/record tests, direct controller/exception-handler invocations, and focused
security/validation tests.

The default context test still inherits the configured external datasource and can
mutate that database; bootstrap seeding is disabled by default, while the opt-in `test`
profile accepts a disposable datasource. A hermetic Testcontainers/default workflow
remains open.

Additional tooling findings:

- Java 25 compilation succeeds, but Lombok and Mockito emit deprecation/dynamic-agent
  warnings.
- `maven-dependency-plugin:3.9.0:analyze` cannot read Java 25 class files (`major
  version 69`) in the current toolchain. With a Java 17 override it runs but produces
  many expected starter/transitive-dependency false positives.
- Flyway runtime dependencies and the separately configured Maven Flyway plugin use
  different version lines: Boot `4.0.6` manages runtime Flyway `11.14.1`, while the
  POM configures the Maven plugin as `12.0.0`. The plugin has no repository-provided
  datasource configuration, and `flyway.conf` is empty. Align or remove the plugin
  before relying on CLI migration commands.
- The composite key warning for `RepairSpareId` is resolved; keep equality/hash-code
  behavior covered as the repair-spare model evolves.

### Confirmed product brief

The following decisions were confirmed with the project owner and should guide the next
implementation slices:

- **Purpose:** internal maintenance operations for a small team; this is not currently
  a public SaaS product.
- **Users:** managers and supervisors log in. There is no public self-registration;
  managers invite users. Technicians do not log in.
- **Departments:** all authenticated users can see all departments, and supervisors can
  perform business operations across departments. Departments are organizational, not
  access boundaries.
- **Administration:** managers alone manage user accounts and the custom machine-status
  catalog. Other operational permissions are shared by managers and supervisors.
- **First usable milestone:** an API-only master-data milestone for Departments,
  Machines, and Spare Parts, including inventory operations and repair integration. No
  web UI is included in this milestone.
- **Machines:** asset identity, equipment details, placement, lifecycle, and maintenance
  metrics are in scope. The status catalog uses five built-in statuses with the agreed
  green/gray/amber/red/slate palette, manager-created custom names/colors, duplicate
  custom names, and UUID-based API references.
- **Inventory:** strict non-negative stock with receive, issue, adjust, and return
  operations. The first version keeps only the current balance; it does not require or
  retain operation reasons or keep a full stock ledger. Parts issued to repairs must be
  linked to the repair. This intentionally means historical stock changes cannot be
  reconstructed; revisit the decision if inventory audit becomes a requirement.
- **Repairs:** scheduled and breakdown repairs use an `OPEN → IN_PROGRESS → COMPLETED`
  lifecycle. Managers and supervisors can create repairs; any authenticated user can
  report a breakdown. A repair may be self-assigned, manager-assigned, claimed by a
  supervisor, or temporarily unassigned.
- **Repair updates:** the assigned supervisor posts append-only status/notes and cost
  entries. Corrections are appended rather than edited. Costs use INR with labor,
  parts, travel, and other categories; manager approval is not required in the first
  version.
- **External technicians:** repairs store the technician name and phone as free text for
  manual communication; a technician directory/login is not part of this milestone.
- **Deletion:** master records are archived/soft-deleted, never hard-deleted. Reusing
  archived identifiers is not part of the first version.
- **Machine status:** repair defaults may change status automatically, but a later
  manual change can override the automatic result.
- **Client:** a web client with useful operational visuals is desired eventually, but
  the current milestone is deliberately API-only.

### Confirmed role matrix

| Capability | Manager | Supervisor | Any authenticated user |
|---|---:|---:|---:|
| View all departments and business records | Yes | Yes | Yes, for reports where applicable |
| Manage departments, machines, and spare-part master data | Yes | Yes | No |
| Receive, issue, adjust, and return stock | Yes | Yes | No |
| Create scheduled repairs | Yes | Yes | No |
| Report a breakdown | Yes | Yes | Yes |
| Assign supervisors to repairs | Yes | Claim/self/manager assignment | No |
| Post assigned-repair updates and costs | Yes | Assigned supervisor | No |
| Invite/manage users | Yes | No | No |
| Create/manage custom machine statuses | Yes | No | No |

The table is the initial authorization contract. Endpoint-level tests must preserve it.

## 3. Working Design Decisions

These decisions are confirmed for the next implementation milestone unless an open
question below explicitly says otherwise.

1. **System roles and scope:** `MANAGER` and `SUPERVISOR` are the login roles. Both can
   operate across all departments; managers alone manage users and the custom status
   catalog. `TECHNICIAN` is not a login role in this milestone.
2. **Authentication:** stateless RSA-signed JWT access and refresh tokens. The signing
   key must move outside source control and application resources.
3. **Registration:** managers invite users. Public self-registration is not a product
   feature and must be removed or disabled.
4. **External technicians:** repairs store a free-text technician name and phone for
   manual communication. A technician directory, login, or vendor workflow is deferred.
5. **First milestone:** API-only Departments, Machines, and Spare Parts master data,
   with inventory operations and repair integration. No web UI is included yet.
6. **Machine state:** the additive V2 status catalog is implemented with five built-in
   statuses, manager-created custom names/colors, duplicate custom names, and UUID API
   references. Machines still use the legacy enum until the next migration. Repair
   lifecycle events may set default status values; a later manual change may override
   the automatic result.
7. **Repair lifecycle:** scheduled and breakdown repairs use `OPEN`, `IN_PROGRESS`, and
   `COMPLETED`. Managers and supervisors can create scheduled repairs; any authenticated
   user can report a breakdown.
8. **Repair responsibility:** a repair may be created unassigned, self-assigned by its
   creating supervisor, assigned by a manager, or claimed by an available supervisor.
   The assigned supervisor posts status/notes and cost updates.
9. **Repair history and costs:** updates and corrections are append-only. Costs are in
   INR and split into labor, parts, travel, and other categories; no manager approval
   is required in the first version.
10. **Inventory:** current stock is the source of truth for the first version. Receive,
    issue, adjust, and return are supported, stock cannot become negative, operation
    reasons are not required or retained, and parts issued to repairs are linked to the
    repair. A full stock ledger and purchase-history module are deferred.
11. **Archive semantics:** master records are archived/soft-deleted and never
    hard-deleted. Archived identifiers are not reused in the first version. `Machine`
    still needs the intended soft-delete mapping; custom `@SQLDelete` behavior must be
    audited and versioned.
12. **Schema ownership:** Flyway owns migrations; JPA uses `ddl-auto=validate` after
    initialization. Once V1 has been accepted, schema changes belong in V2+ migrations.
13. **Auditing:** entities use `BaseEntity` for UUIDv7 IDs and audit timestamps, but
    versioning and auditing are not consistently enforced across the model.
## 4. Implementation Inventory

### Implemented endpoints

| Endpoint | Current behavior | Status |
|---|---|---|
| `POST /api/v1/auth/register` | No longer mapped; public registration was removed | **Removed** |
| `POST /api/v1/auth/login` | Database-backed BCrypt authentication; returns tokens | Implemented |
| `POST /api/v1/auth/refresh` | Refresh-only decoder rotates a hashed, family-bound token | Implemented: rotation/replay revocation and cleanup |
| `POST /api/v1/auth/logout` | Authenticated refresh-token revocation; returns 204 | Implemented |
| `POST /api/v1/users` | Manager-only; creates a `SUPERVISOR` account | Implemented, initial slice |
| `GET /api/v1/machine-statuses` | Lists built-in and active custom statuses | Implemented |
| `POST /api/v1/machine-statuses` | Manager-only; creates a custom status | Implemented, creation-only |
| `GET /api/v1/machines` | Lists active machines with optional department/status filters | Implemented |
| `GET /api/v1/machines/{id}` | Reads one active machine | Implemented |
| `POST /api/v1/machines` | Creates a machine with default/custom catalog status | Implemented |
| `PATCH /api/v1/machines/{id}` | Updates machine master data and status | Implemented |
| `DELETE /api/v1/machines/{id}` | Archives a machine and returns 204 | Implemented |
| `GET /api/v1/spares` | Lists active spare-part records | Implemented |
| `GET /api/v1/spares/{id}` | Reads one active spare | Implemented |
| `POST /api/v1/spares` | Creates a spare with catalog fields and initial stock | Implemented |
| `PATCH /api/v1/spares/{id}` | Updates spare catalog fields | Implemented |
| `DELETE /api/v1/spares/{id}` | Archives a spare and returns 204 | Implemented |
| `POST /api/v1/spares/{id}/stock/receive` | Adds received quantity to the current balance | Implemented |
| `POST /api/v1/spares/{id}/stock/return` | Returns quantity from a repair and restores balance | Implemented |
| `POST /api/v1/spares/{id}/stock/adjust` | Sets the absolute nonnegative balance | Implemented |
| `POST /api/v1/spares/{id}/stock/issue` | Decrements balance and links usage to a repair | Implemented |
| `GET /api/v1/repairs` | Lists repairs with optional status/machine filters | Implemented |
| `GET /api/v1/repairs/{id}` | Reads one repair | Implemented |
| `POST /api/v1/repairs` | Creates scheduled/breakdown repairs with idempotency and supervisor self-assignment | Implemented |
| `PATCH /api/v1/repairs/{id}` | Updates repair master data | Implemented |
| `POST /api/v1/repairs/{id}/claim` | Allows an unassigned supervisor to claim a repair | Implemented |
| `PATCH /api/v1/repairs/{id}/assignment` | Manager-only assignment or temporary unassignment | Implemented |
| `GET /api/v1/repairs/{id}/updates` | Lists append-only status/note history | Implemented |
| `POST /api/v1/repairs/{id}/updates` | Appends a status/note entry and advances the lifecycle | Implemented |
| `GET /api/v1/repairs/{id}/costs` | Lists append-only INR cost history | Implemented |
| `POST /api/v1/repairs/{id}/costs` | Appends a labor/parts/travel/other INR cost | Implemented |
| `GET /api/v1/health` | Authenticated principal echo, not a real health check | Implemented |
| `GET /api/v1/departments` | Lists active departments | Implemented |
| `POST /api/v1/departments` | Creates a department with DB-backed duplicate handling | Implemented |
| `PATCH /api/v1/departments/{id}` | Updates by ID and allows rename | Implemented |
| `DELETE /api/v1/departments/{id}` | Archives a department and returns 204 | Implemented |

### Persistence model present but not wired into workflows

- `Department`, `User`, `Machine`, `Technician`, `Repair`, `RepairUpdate`, `RepairCost`,
  `Spare`, and `RepairSpare` entities exist.
- `UserRepository` and `DepartmentRepository` are used.
- `UserManagementService` and `UserController` provide the initial manager-only
  supervisor invitation path.
- `MachineStatusService` and `MachineStatusController` provide the V2 status catalog.
- `MachineService` and `MachineController` provide active machine CRUD, filters, status
  assignment, and archive semantics.
- `SpareService` and `SpareController` provide spare master data and current-balance
  inventory operations.
- `RepairService` and `RepairController` provide repair creation/listing, idempotency,
  manager assignment, supervisor claim, append-only updates, and INR cost entries.
- `RepairUpdateRepository` and `RepairCostRepository` provide append-only history reads.
- `RepairSpareRepository` supports repair-linked spare usage.
- `RefreshTokenService` and `RefreshTokenRepository` store only SHA-256 hashes, rotate
  token families under a pessimistic lock, and revoke replays.
- The legacy `Technician` entity remains dormant; repairs use free-text technician fields.
- There is no password lifecycle; expired refresh-token cleanup is implemented.

### Configuration and schema

- `spring.flyway.enabled=true` and `spring.jpa.hibernate.ddl-auto=validate` are now
  active.
- `application-prod.properties` limits Actuator exposure to health/info and disables
  Swagger/OpenAPI; local defaults remain available for development.
- `V1__init_schema.sql` has been reduced to one table/index/constraint block.
- V2 adds an additive `machine_statuses` catalog and seeds the five built-in statuses.
- V3 backfills existing machines, replaces the legacy status column with `status_id`,
  and adds the catalog foreign key; it requires a controlled migration window because
  the old column is removed.
- V4 adds machine equipment, placement, lifecycle, and maintenance fields.
- V5 adds spare description, unit, and machine-compatibility fields.
- V6 adds repair supervisor assignment, free-text external technician fields, and the
  append-only `repair_costs` table.
- V7 adds nonnegative stock/usage and lifecycle-date checks.
- V8 adds the hashed refresh-token/family persistence table.
- V1–V8 were validated and applied successfully against the authorized test PostgreSQL
  database; repeatable Testcontainers coverage is still pending.
- V1 was changed in the latest commit; environments that applied an earlier checksum
  may require an explicit repair/baseline procedure.
- The schema lacks important invariants for nonnegative stock, positive spare usage,
  date consistency, and non-null roles.

## 5. Prioritized Risk Register

### P0 — Security containment

#### P0.1 Rotate and externalize secrets

**Finding:** `private.key` and `public.key` are tracked under
`src/main/resources/private.key` and `src/main/resources/public.key`. The packaged JAR
contains both files. Historical commits also contain the Jasypt master password
alongside encrypted database credentials, so those credentials must be treated as
compromised even though the active properties file no longer contains the master
password.

**Tasks:**

- [ ] Generate a new RSA key pair and invalidate all tokens signed by the old key.
- [ ] Move signing material outside the repository and outside the application JAR;
      load it through an environment/secret-manager mechanism.
- [ ] Rotate the database credential and any historical Jasypt material, and
      externalize the complete environment-specific datasource configuration.
- [ ] Review access logs and persisted data for tokens or credentials issued before
      rotation; investigate any suspected misuse.
- [ ] Remove key files and local secret files from Git tracking and add appropriate
      `.gitignore` rules.
- [ ] Coordinate history cleanup with the owner; deleting current files alone does not
      remove historical exposure.
- [ ] Document key rotation and emergency invalidation procedures.

**Acceptance:** no private key or real credential is present in a clean checkout,
artifact, log, or test fixture; a newly generated key is required at runtime; and
existing deployments have been audited and rotated rather than only fresh deployments
being protected.

#### P0.2 Remove predictable bootstrap credentials

**Finding:** `DatabaseInitializer` is now disabled by default and requires explicit
operator-supplied department/manager properties when `app.bootstrap.enabled=true`; no
personal seed identity or predictable password remains in source.

**Tasks:**

- [x] Disable automatic production seeding; it is now opt-in via
      `app.bootstrap.enabled=true`.
- [x] Use externally supplied bootstrap properties rather than source literals.
- [ ] Rotate any previously seeded manager credentials in existing deployments.
- [ ] Audit and disable or investigate accounts created through public registration.
- [x] Remove hard-coded personal seed data from the source and deployment process.
- [ ] Add a forced password-change/credential-rotation path.
- [ ] Make initialization safe under concurrent application instances and when all
      existing users are soft-deleted.

**Acceptance:** neither a fresh nor an existing deployment can be accessed with
credentials published in source or history.

#### P0.3 Close the public registration path

**Finding:** public registration was removed from the controller/service, and only exact
login/refresh POST routes are public. The manager invitation endpoint exists, and
representative HTTP 401/403 coverage now spans user, status, stock, and repair routes;
the complete role matrix remains pending.

**Tasks:**

- [x] Remove the public registration endpoint and its service flow; manager invitation
      is the confirmed account-creation path.
- [x] Replace wildcard `/api/v1/auth/**` matching with exact public matchers for
      login and refresh; a future logout endpoint must not inherit `permitAll`.
- [x] Add a manager-only `POST /users` flow with server-side role validation.
- [x] Add the custom JWT authority converter required by the manager-only rule.
- [x] Replace tests that previously required public supervisor registration.
- [x] Add isolated HTTP security tests proving 401/403 behavior for the user endpoint.

**Acceptance:** unauthenticated registration is rejected; only an authorized manager
can create users, with documented role and department rules, and 401/403 behavior is
covered by HTTP security tests.

### P1 — Authentication and authorization correctness

#### P1.1 Enforce an explicit role matrix

**Finding:** URL-level authorization now protects manager-only user/status routes,
manager/supervisor machine and stock mutations, and the role-specific repair mutation
boundaries; authenticated users can read business records. Method-level authorization is
not enabled, and the complete role matrix is not yet covered by HTTP tests.

- [x] Implement the initial confirmed role matrix for the user route: managers and
      supervisors share business operations; manager-only actions cover user management.
- [ ] Enable method security after checking the current AOP auto-configuration exclusion.
- [x] Add a custom JWT authority converter so `ROLE_MANAGER` and `ROLE_SUPERVISOR`
      claims map to Spring role authorities.
- [x] Add MockMvc/security tests for 401, 403, and representative
      manager/supervisor/reporter boundaries across user, status, stock, and repair routes.
- [ ] Complete the role matrix coverage for every protected operation.

#### P1.2 Separate access and refresh token validation

**Finding:** access and refresh decoders are separate and validate issuer, purpose, and
refresh-token `jti`; audience policy and broader required-claim policy remain open.

- [x] Add an access-token validator requiring the correct signature, issuer, and token
      type.
- [x] Use separate refresh validation rules.
- [x] Add a random `jti` to refresh tokens and persist only their hashes.
- [x] Test that access tokens cannot access protected endpoints and refresh tokens
      cannot be accepted by the access decoder.

#### P1.3 Add refresh rotation, revocation, and logout

- [x] Add a `RefreshToken` persistence model storing a hash and `jti`, not a raw
      bearer token.
- [x] Rotate refresh tokens atomically on every successful refresh.
- [x] Reject replay of a consumed token and revoke token families when reuse is
      detected.
- [x] Add `POST /api/v1/auth/logout` and scheduled expiry cleanup for refresh-token rows.
- [ ] Decide how user deactivation invalidates existing access tokens.

#### P1.4 Add abuse controls and safe diagnostics

- [ ] Add login/refresh and breakdown-report throttling; the current Resilience4j
      dependency is unused and is not Spring-integrated.
- [x] Restrict Actuator `startup`/`conditions` and Swagger/OpenAPI in the `prod` profile.
- [x] Fix CORS methods and allowed headers for the current API; exact approved origins
      remain unchanged.
- [x] Return stable JSON 401/403 responses from the security filter chain.
- [ ] Review diagnostics/logging to ensure credentials and tokens are not emitted.

### P1 — Build, test, and migration safety

#### P1.5 Isolate the test environment

**Finding:** the default full context test still passes against the authorized test
database and V1–V8 are verified there, so it can still mutate that database. Bootstrap
seeding is disabled by default; an opt-in `test` profile accepts `TEST_DATABASE_URL`,
`TEST_DATABASE_USERNAME`, and `TEST_DATABASE_PASSWORD`, and runs the same
migration/context check against a caller-provided disposable database. A hermetic
default/Testcontainers workflow is still required.

- [x] Add an opt-in test profile and test-only datasource configuration.
- [ ] Use Testcontainers PostgreSQL or a deliberately disposable local database.
- [x] Disable the bootstrap initializer in the opt-in integration profile.
- [ ] Inventory each deployment database's `flyway_schema_history` before choosing
      between preserving V1, baselining, or creating V2+; do not blindly run `repair`
      to hide checksum or schema mismatches.
- [ ] Align the runtime and Maven-plugin Flyway versions, or remove the unused Maven
      plugin and document the supported migration workflow.
- [ ] Add clean-database migration tests in addition to the successful V1→V8 test-DB
      run.
- [ ] Make `mvn verify` safe by default.

**Acceptance:** a clean checkout can run the complete test suite without network access
to the configured application database, and migration behavior is reproducible for a
new and an existing database.

#### P1.6 Add real HTTP, security, and persistence tests

The current 336-test inventory still overstates behavioral coverage. Add:

- `MockMvc`/`WebTestClient` tests for routing, JSON binding, validation, status codes,
  CORS, and the security filter chain.
- [x] Real RSA encode/decode tests covering tampering, expiry, issuer, and token purpose.
- [x] Add repository integration coverage for soft deletion, archive filtering, and audit fields.
- [ ] Add repository coverage for optimistic locking and uniqueness races.
- [x] Add rollback-only PostgreSQL workflow coverage for repair, stock, and cost transactions.
- [ ] Add clean-database Flyway migration coverage independent of the configured test database.
- Tests for `DataIntegrityViolationException`, malformed JSON, null values, and
  optimistic-lock failures.

### P1 — API and domain correctness

- [x] Add the Jakarta Validation starter and constraints to the current request DTOs.
- [x] Apply `@Valid` at current controller boundaries and map validation errors to a
      stable 400 response.
- [x] Map persistence conflicts and optimistic-lock failures to non-leaking 409 responses.
- [x] Return stable JSON 401/403 responses from Spring Security.
- [ ] Replace the remaining catch-all 500 behavior and log unexpected server errors with
      context.
- [ ] Decide whether to use `ProblemDetail` or retain the current `ErrorResponse`
      contract consistently.
- [x] Redesign Department routes to use plural resources and path IDs.
- [x] Make Department creation concurrency-safe by translating the database uniqueness
      race to HTTP 409.
- [x] Allow Department rename through an explicit PATCH contract.
- [x] Return proper 404/409/204 semantics for Department operations.
- [ ] Remove secret-bearing DTO `toString()` output or prevent request/response DTOs
      from being logged.

### P1 — Data-model corrections

- [x] Apply active filtering and explicit audited archive behavior to `Machine`; avoid
      the legacy custom `@SQLDelete` path.
- [x] Apply explicit archive behavior to `Department`; avoid its legacy custom
      `@SQLDelete` path.
- [ ] Decide whether deleted `User`, `Department`, and `Spare` rows may be recreated;
      current unique constraints prevent reuse even when repository queries hide those
      deleted rows. Handle `Machine` separately after its soft-delete behavior is fixed.
- [x] Add the manager-managed status catalog with built-in statuses, custom
      names/colors, duplicate custom names, and UUID API references.
- [x] Link `Machine` records to the catalog, replace the legacy enum/check constraint,
      and add active-machine archive behavior.
- [x] Add the initial equipment, placement, lifecycle, and maintenance master fields.
- [x] Implement the confirmed current-balance inventory model: receive, issue, adjust,
      and return without a required reason, with nonnegative stock and repair links for
      issued parts; do not add a full ledger in this milestone.
- [x] Store external technician name and phone as free text on repairs; do not make the
      dormant `Technician` entity a prerequisite for the first milestone.
- [x] Remove update-history cascade deletion, mark `RepairUpdate`/`RepairCost`
      immutable, and expose only append/read operations.
- [x] Add `equals`/`hashCode` to `RepairSpareId`.
- [x] Separate `Spare.lastPurchaseDate` from general update auditing, even though full
      purchase history is deferred.
- [x] Add database checks for nonnegative stock/usage and repair/machine date consistency;
      role non-null enforcement remains open.
- [ ] Review foreign-key indexes and remove indexes duplicated by unique constraints.
- [ ] Define audited, versioned soft-delete behavior for the remaining legacy entities;
      `User` still has a custom `@SQLDelete` statement.
- [ ] Decide where `@Version` belongs and ensure history entities have the intended
      concurrency behavior.
- [x] Fix `AuditorAware` handling of anonymous authentication so unauthenticated
      operations do not silently become `anonymousUser`.

## 6. Feature Delivery Plan

Feature work starts only after the P0/P1 security and test foundations are accepted.
The first usable milestone is deliberately API-only; the web client is a later product
slice, not part of the initial acceptance criteria.

### Milestone 1 — Master data and repair-ready API

This is the first business milestone.

- **Departments:** create, read, update, archive, and list endpoints with identity/POC,
  contact channels, location, status, and notes fields.
- **Machines:** create, read, update, archive, list, and filter endpoints with asset
  identity, equipment details, placement, lifecycle, and maintenance metrics.
- **Machine statuses:** built-in operational/idle/under-maintenance/fault/decommissioned
  values plus a manager-managed catalog of custom names and colors.
- **Spare parts:** create, read, update, archive, list, and compatibility endpoints with
  part identity, description, unit, and machine compatibility.
- **Inventory:** current-balance receive, issue, adjust, and return operations; do not
  require or retain operation reasons, reject negative results, and link issued quantities
  to a repair.
- **Repairs:** scheduled and breakdown creation, list/read, status updates, assignment
  or claim, and idempotency handling.
- **Repair costs:** append-only INR entries for labor, parts, travel, and other costs;
  no manager approval is required in this milestone.
- **External technicians:** accept free-text technician name and phone on a repair; do
  not require a `Technician` login or directory record.
- **Authorization:** enforce the confirmed role matrix, including manager-only user and
  custom-status administration.
- **Acceptance:** the API supports a manager and supervisor completing the full flow
  from master-data setup through repair creation, status update, cost entry, and spare
  issue; an authenticated user can submit a breakdown; all writes are validated and
  authorized, master-data records are archived rather than hard-deleted, repair history
  is append-only, and the flow is covered by isolated tests.

Current progress: manager user invitation, Department CRUD, request validation, JWT
role mapping, token-purpose separation, hashed refresh-token rotation/revocation/logout and
cleanup, operator-supplied opt-in bootstrap values, CORS, the V2–V8
status/machine/spare/repair/auth foundation, Machine CRUD,
Spare CRUD, current-balance inventory, repair creation, assignment, lifecycle updates,
INR costs, repair-driven machine status defaults, representative HTTP security
coverage with JSON 401/403 errors, rollback-only PostgreSQL workflow coverage,
spare purchase-date audit coverage, repository archive/audit coverage, and an opt-in
disposable-database profile are implemented. V1–V8 pass against the authorized
test database; the opt-in profile is skipped without its three environment variables,
and hermetic Testcontainers coverage, secret rotation, and the complete role/security
matrix remain open.

### Milestone 2 — Repair lifecycle and machine-state automation

- [x] Enforce `OPEN → IN_PROGRESS → COMPLETED` transitions and append-only corrections.
- [x] Implement manager assignment, supervisor self-assignment, and supervisor claiming.
- [x] Apply repair-default machine status changes: breakdown/open work can set fault or
      under-maintenance, and completion can set operational.
- [ ] Allow a later authorized manual status change to override the automatic result.
- [ ] Add transaction tests for status, cost, assignment, and spare-issue races.

### Milestone 3 — Inventory evolution

- Add a full immutable stock ledger only if the current-balance model proves
  insufficient; this is not required for the first milestone.
- Add purchase history, supplier data, unit costs, reorder levels, and low-stock
  reporting as a separately approved scope.
- Add pessimistic locking and concurrency tests if stock becomes a high-contention
  operational concern.

### Milestone 4 — Web client and operational visuals

- Define the client stack and deployment boundary after the API is stable.
- Build master-data, repair, stock, and low-stock views.
- Add operational infographics such as machine status, repair backlog, repair age,
  stock levels, and cost trends.
- Keep visualization metrics traceable to API data rather than embedding business logic
  in the client.

### Milestone 5 — Release hardening

- Restrict diagnostics and documentation endpoints by profile/environment.
- Decide whether Resilience4j is required and implement it properly or remove it.
- Resolve the supported JDK baseline; Java 25 LTS is now selected. Pin the selected
  JDK in CI and add a Maven Wrapper (or another reproducible Maven version constraint).
- Add CI for the selected JDK/Maven versions, the isolated test suite, packaging,
  dependency/security scanning, and migration validation.
- Add a README covering setup, secret injection, database migration, bootstrap flow,
  endpoint usage, and test commands.
- Update this roadmap as decisions and acceptance evidence become available.

## 7. Test and Quality Gates

A change is not complete merely because a unit test passes. The minimum gate for a
vertical slice is:

- [x] Clean compile with the selected Java 25 LTS baseline.
- [ ] Relevant unit tests.
- [ ] MVC/security tests for the public contract.
- [ ] PostgreSQL integration coverage for persistence and migrations.
- [ ] Validation and error-response tests.
- [ ] Authorization tests for every protected operation.
- [ ] Transaction/concurrency tests where writes can race.
- [ ] No secrets in source, fixtures, reports, logs, or packaged artifacts.
- [ ] `git diff --check` and a clean, intentional worktree diff.

The canonical command has now passed against the authorized test database:

```bash
mvn -B -ntp clean verify
```

It is still not safe on an arbitrary checkout until P1.5 provides an isolated test
datasource. Without that isolation, the full command may mutate the configured
application database; the explicit exclusion is a **database-contact avoidance**
workaround only:

```bash
mvn -B -ntp -Dtest='!MaintainsoftApplicationTests' clean verify
```

That workaround still copies the tracked key resources into the packaged artifact until
P0.1 is closed; do not distribute the resulting JAR.

## 8. Non-Goals and Guardrails

- Do not deploy the current artifact to an untrusted network.
- Do not reintroduce public self-registration without an explicit product decision and
  abuse controls.
- Do not add a third login role; technicians remain external free-text contacts in this
  milestone, and a technician directory/login is deferred.
- Do not commit real keys, passwords, encrypted credentials, or local environment files.
- Do not edit an already-applied Flyway migration; create V2+ migrations.
- Do not switch JPA back to `update`/`create` after Flyway is enabled.
- Do not expose Actuator diagnostics or API documentation publicly in production
  without an explicit decision.
- Do not add machine/repair/spare features before security, test isolation, and the
  role matrix are accepted.
- Do not weaken CORS or CSRF assumptions without documenting the deployment model.

## 9. Open Decisions for the Project Owner

1. Where should RSA keys and database/Jasypt secrets be stored in deployment?
2. Which frontend/client stack and CORS origins will be used after the API milestone?
3. Should refresh tokens use a database table, Redis, or another session store?
4. Is rate limiting intended only for auth/breakdown endpoints or for the broader API?
5. What recurrence/lead-time rules apply to scheduled repairs?
6. When is a repair considered complete when stock or cost information is incomplete?
7. Where is the missing `DESIGN.md`, if it still exists?
8. Can archived master records be restored, and should archived identifiers ever be
   reused after the first version?
9. Can custom machine statuses be edited or archived after creation, or remain
   creation-only for the first milestone?
10. Java 25 is the selected project baseline; CI/toolchain pinning remains open.

## 10. Definition of Ready for Feature Development

Feature work may begin when:

- P0.1–P0.3 are closed and secrets are rotated.
- The role matrix and registration decision are documented and enforced.
- P1.1–P1.6 security, token, test-isolation, and HTTP/persistence coverage gates are
  closed or explicitly deferred by the owner.
- P1 API and data-model corrections are either implemented or explicitly accepted as
  documented follow-up work.
- The test suite is isolated from the configured application database.
- Flyway has been verified from an empty disposable PostgreSQL database.
- Access/refresh token separation and revocation are implemented and tested.
- Validation, authorization, and error semantics are defined for existing endpoints.

Until then, this project should be treated as an actively developed backend prototype,
not a production service.
