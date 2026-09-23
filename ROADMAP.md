# MaintainSoft — Engineering Roadmap

> **Source baseline:** commit `493f905` (`master` / `origin/master`), verified 2026-09-23.
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
parts, but the implemented application is still a small auth/department vertical slice.

**Current release posture: not production-ready.** The next work should be security
containment, reproducible testing, and authorization—not new feature breadth.

The most urgent facts are:

- A JWT RSA private key is tracked under `src/main/resources` and is packaged into the
  application JAR. It must be treated as compromised and rotated.
- The bootstrap manager uses hard-coded, publicly known credentials.
- Public registration is enabled and creates active supervisor accounts.
- No role-level authorization exists; every authenticated user can mutate departments.
- Refresh tokens can be accepted as access tokens, and cannot be revoked or rotated.
- The only Spring context test uses the configured external database and can run Flyway
  plus the bootstrap initializer. It is not safe for routine test execution.
- The current source compiles on Java 26 and 202 non-context tests pass, but migration,
  MVC/security, repository, and concurrency behavior are not integration-tested.

## 2. Current Baseline

### Technology

- Spring Boot `4.0.6`
- Java release `26`
- Maven (verified with Maven `3.9.16` and Temurin `26.0.2.1`)
- Spring MVC, Spring Data JPA/Hibernate, PostgreSQL
- Spring Security OAuth2 resource server with hand-issued RSA JWTs
- Flyway, Jasypt, Actuator, Springdoc, Lombok
- Resilience4j rate-limiter dependency is present but unused

### Repository shape

- Package root: `com.maintainsoft`
- Conventional layers: `controller`, `service`, `repository`, `entity`, `dto`,
  `enums`, `exception`, `security`
- No frontend is currently present. Earlier Vaadin/Next.js experiments were removed.
- No Maven wrapper, CI workflow, Docker/deployment descriptor, test resources, or
  `README.md` is currently committed.
- `flyway.conf` is present but empty and is not a substitute for a configured
  environment-specific migration setup.

### Verified build and test state

The following checks were run with Java 26:

```text
mvn -B -ntp -Dtest='!MaintainsoftApplicationTests' clean verify
Result: BUILD SUCCESS — 202 tests passed
```

A test-skipping package build also succeeds. The 202 passing tests are mostly
Mockito unit tests, accessor/record tests, and direct controller/exception-handler
invocations.

The remaining test is:

```text
src/test/java/com/maintainsoft/MaintainsoftApplicationTests.java
```

It is intentionally excluded from routine runs because it has no test profile,
embedded database, Testcontainers setup, or isolated datasource. It can connect to the
configured PostgreSQL instance, run Flyway, validate JPA mappings, and execute
`DatabaseInitializer`.

Additional tooling findings:

- Java 26 compilation succeeds, but Lombok and Mockito emit deprecation/dynamic-agent
  warnings.
- `maven-dependency-plugin:3.9.0:analyze` cannot read Java 26 class files (`major
  version 70`) in the current toolchain. With a Java 17 override it runs but produces
  many expected starter/transitive-dependency false positives.
- Flyway runtime dependencies and the separately configured Maven Flyway plugin use
  different version lines: Boot `4.0.6` manages runtime Flyway `11.14.1`, while the
  POM configures the Maven plugin as `12.0.0`. The plugin has no repository-provided
  datasource configuration, and `flyway.conf` is empty. Align or remove the plugin
  before relying on CLI migration commands.

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
  metrics are in scope. Built-in statuses plus manager-created custom statuses with
  names and colors are required.
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
6. **Machine state:** built-in statuses plus manager-created custom statuses with names
   and colors. Repair lifecycle events may set default status values; a later manual
   change may override the automatic result.
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
| `POST /api/v1/auth/register` | Public; creates a `SUPERVISOR`; returns tokens | **Unsafe / remove** |
| `POST /api/v1/auth/login` | Database-backed BCrypt authentication; returns tokens | Implemented, needs security tests |
| `POST /api/v1/auth/refresh` | Decodes a refresh JWT and issues a new pair | Incomplete and replayable |
| `GET /api/v1/health` | Authenticated principal echo, not a real health check | Implemented |
| `GET /api/v1/departments` | Lists active departments | Implemented |
| `POST /api/v1/department` | Creates a department after a check-then-insert | Race-prone |
| `PATCH /api/v1/department` | Updates by department name; cannot rename | Contract needs redesign |
| `DELETE /api/v1/department?id=` | Soft-deletes or returns an error message with HTTP 200 | Contract needs redesign |

### Persistence model present but not wired into workflows

- `Department`, `User`, `Machine`, `Technician`, `Repair`, `RepairUpdate`, `Spare`, and
  `RepairSpare` entities exist.
- `UserRepository` and `DepartmentRepository` are used.
- `MachineRepository` and `SpareRepository` are currently dormant.
- There are no technician, machine, spare, repair, repair-update, or repair-spare
  services/controllers.
- There is no `POST /users`, logout, password lifecycle, or refresh-token repository.

### Configuration and schema

- `spring.flyway.enabled=true` and `spring.jpa.hibernate.ddl-auto=validate` are now
  active.
- `V1__init_schema.sql` has been reduced to one table/index/constraint block.
- The migration has not been proven against a disposable empty PostgreSQL database.
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

**Finding:** `DatabaseInitializer` creates a manager when the user table is empty using
hard-coded identity and password values.

**Tasks:**

- [ ] Disable automatic production seeding or make it profile-controlled.
- [ ] Use a one-time, externally supplied bootstrap secret or an explicit operator
      command.
- [ ] Rotate any previously seeded manager credentials in existing deployments.
- [ ] Audit and disable or investigate accounts created through public registration.
- [ ] Remove hard-coded personal seed data from the source and deployment process.
- [ ] Add a forced password-change/credential-rotation path.
- [ ] Make initialization safe under concurrent application instances and when all
      existing users are soft-deleted.

**Acceptance:** neither a fresh nor an existing deployment can be accessed with
credentials published in source or history.

#### P0.3 Close the public registration path

**Finding:** `/api/v1/auth/**` is public and registration immediately creates a usable
supervisor account.

**Tasks:**

- [ ] Remove the public registration endpoint and its service flow; manager invitation
      is the confirmed account-creation path.
- [ ] Replace wildcard `/api/v1/auth/**` matching with exact public matchers for
      login and refresh; a future logout endpoint must not inherit `permitAll`.
- [ ] Add a manager-only `POST /users` flow with server-side role validation.
- [ ] Sequence the role converter and method/URL authorization work in P1.1 before
      accepting the manager-only flow; the current JWT authority mapping is not yet
      compatible with `hasRole('MANAGER')`.
- [ ] Replace tests that currently require public supervisor registration.

**Acceptance:** unauthenticated registration is rejected; only an authorized manager
can create users, with documented role and department rules, and 401/403 behavior is
covered by HTTP security tests.

### P1 — Authentication and authorization correctness

#### P1.1 Enforce an explicit role matrix

**Finding:** the security chain ends at `anyRequest().authenticated()`. No method-level
authorization exists.

- [ ] Implement the confirmed role matrix: managers and supervisors share business
      operations across all departments; manager-only actions cover user management and
      custom status configuration.
- [ ] Enable method security only after checking the current AOP auto-configuration
      exclusion.
- [ ] Add a custom JWT authority converter. The current token writes `ROLE_MANAGER`
      into the OAuth `scope` claim; the default resource-server converter would normally
      produce `SCOPE_ROLE_MANAGER`, not `ROLE_MANAGER`.
- [ ] Add MockMvc/security tests for 401, 403, and the complete role matrix.

#### P1.2 Separate access and refresh token validation

**Finding:** the same decoder is used by the resource server and refresh endpoint, but
the resource server does not require `type=access`. A valid refresh token can therefore
authenticate a bearer request.

- [ ] Add an access-token validator requiring the correct signature, issuer, token
      type, required claims, and (if applicable) audience.
- [ ] Use separate refresh validation rules.
- [ ] Add a random `jti` to refresh tokens.
- [ ] Test that access tokens cannot refresh and refresh tokens cannot access protected
      endpoints.

#### P1.3 Add refresh rotation, revocation, and logout

- [ ] Add a `RefreshToken` persistence model, preferably storing a hash or `jti`, not a
      raw bearer token.
- [ ] Rotate refresh tokens atomically on every successful refresh.
- [ ] Reject replay of a consumed token and revoke token families when reuse is
      detected.
- [ ] Add `POST /api/v1/auth/logout` and an expiry cleanup strategy.
- [ ] Decide how user deactivation invalidates existing access tokens.

#### P1.4 Add abuse controls and safe diagnostics

- [ ] Add login/refresh and breakdown-report throttling; the current Resilience4j
      dependency is unused and is not Spring-integrated.
- [ ] Restrict Actuator `startup`/`conditions` and Swagger/OpenAPI by environment.
- [ ] Fix CORS methods (`PATCH` and `DELETE` are currently omitted), allowed headers,
      and the exact development origins.
- [ ] Configure explicit security error responses and avoid logging credentials/tokens.

### P1 — Build, test, and migration safety

#### P1.5 Isolate the test environment

**Finding:** the full context test inherits production-like configuration and can mutate
the configured database.

- [ ] Add a test profile and test-only datasource configuration.
- [ ] Use Testcontainers PostgreSQL or a deliberately disposable local database.
- [ ] Disable the bootstrap initializer in ordinary integration tests.
- [ ] Inventory each deployment database's `flyway_schema_history` before choosing
      between preserving V1, baselining, or creating V2+; do not blindly run `repair`
      to hide checksum or schema mismatches.
- [ ] Align the runtime and Maven-plugin Flyway versions, or remove the unused Maven
      plugin and document the supported migration workflow.
- [ ] Add tests for Flyway from an empty database, JPA validation, and schema history.
- [ ] Make `mvn verify` safe by default.

**Acceptance:** a clean checkout can run the complete test suite without network access
to the configured application database, and migration behavior is reproducible for a
new and an existing database.

#### P1.6 Add real HTTP, security, and persistence tests

The current 203-test inventory overstates behavioral coverage. Add:

- `MockMvc`/`WebTestClient` tests for routing, JSON binding, validation, status codes,
  CORS, and the security filter chain.
- Real RSA encode/decode tests covering tampering, expiry, issuer, audience, and token
  purpose.
- Repository tests for soft deletion, optimistic locking, uniqueness, and audit fields.
- PostgreSQL integration tests for Flyway and transaction behavior.
- Tests for `DataIntegrityViolationException`, malformed JSON, null values, and
  optimistic-lock failures.

### P1 — API and domain correctness

- [ ] Add the Jakarta Validation starter and constraints to request DTOs.
- [ ] Apply `@Valid` at controller boundaries and map validation errors to a stable
      400 response.
- [ ] Replace the catch-all 500 behavior with specific mappings for 400, 401, 403, 404,
      409, and optimistic-lock failures; log unexpected server errors with context.
- [ ] Decide whether to use `ProblemDetail` or retain the current `ErrorResponse`
      contract consistently.
- [ ] Redesign Department routes to use plural resources and path IDs.
- [ ] Make Department creation concurrency-safe by translating the database uniqueness
      race to HTTP 409.
- [ ] Allow Department rename through an explicit PATCH contract.
- [ ] Return proper 404/409/204 semantics instead of HTTP 200 error messages.
- [ ] Remove secret-bearing DTO `toString()` output or prevent request/response DTOs
      from being logged.

### P1 — Data-model corrections

- [ ] Apply the intended soft-delete mapping to `Machine`.
- [ ] Decide whether deleted `User`, `Department`, and `Spare` rows may be recreated;
      current unique constraints prevent reuse even when repository queries hide those
      deleted rows. Handle `Machine` separately after its soft-delete behavior is fixed.
- [ ] Replace the fixed machine-status enum/check constraint with a manager-managed
      status catalog supporting built-in statuses plus custom names and colors.
- [ ] Implement the confirmed current-balance inventory model: receive, issue, adjust,
      and return without a required reason, with nonnegative stock and repair links for
      issued parts; do not add a full ledger in this milestone.
- [ ] Store external technician name and phone as free text on repairs; do not make the
      dormant `Technician` entity a prerequisite for the first milestone.
- [ ] Remove `REMOVE` from the `RepairUpdate` cascade (not only orphan removal),
      prohibit repair-history deletion or define archival semantics, and expose only an
      append operation.
- [ ] Add `equals`/`hashCode` to `RepairSpareId`.
- [ ] Separate `Spare.lastPurchaseDate` from general update auditing, even though full
      purchase history is deferred.
- [ ] Add database checks for stock, repair-spare quantities, roles, and repair date
      consistency.
- [ ] Review foreign-key indexes and remove indexes duplicated by unique constraints.
- [ ] Define audited, versioned soft-delete behavior; the current custom `@SQLDelete`
      statements only set `deleted` and do not update `updated_at`, `updated_by`, or the
      stored version.
- [ ] Decide where `@Version` belongs and ensure history entities have the intended
      concurrency behavior.
- [ ] Fix `AuditorAware` handling of anonymous authentication so unauthenticated
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

### Milestone 2 — Repair lifecycle and machine-state automation

- Enforce `OPEN → IN_PROGRESS → COMPLETED` transitions and append-only corrections.
- Implement manager assignment, supervisor self-assignment, and supervisor claiming.
- Apply repair-default machine status changes: breakdown/open work can set fault or
  under-maintenance, and completion can set operational.
- Allow a later authorized manual status change to override the automatic result.
- Add transaction tests for status, cost, assignment, and spare-issue races.

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
- Resolve the supported JDK baseline; the current project target is Java 26, while the
  LTS decision remains open. Pin the selected JDK in CI and add a Maven Wrapper (or
  another reproducible Maven version constraint).
- Add CI for the selected JDK/Maven versions, the isolated test suite, packaging,
  dependency/security scanning, and migration validation.
- Add a README covering setup, secret injection, database migration, bootstrap flow,
  endpoint usage, and test commands.
- Update this roadmap as decisions and acceptance evidence become available.

## 7. Test and Quality Gates

A change is not complete merely because a unit test passes. The minimum gate for a
vertical slice is:

- [ ] Clean compile with the currently selected JDK (currently Java 26; confirm whether
      an LTS baseline should replace it).
- [ ] Relevant unit tests.
- [ ] MVC/security tests for the public contract.
- [ ] PostgreSQL integration coverage for persistence and migrations.
- [ ] Validation and error-response tests.
- [ ] Authorization tests for every protected operation.
- [ ] Transaction/concurrency tests where writes can race.
- [ ] No secrets in source, fixtures, reports, logs, or packaged artifacts.
- [ ] `git diff --check` and a clean, intentional worktree diff.

After P1.5 provides an isolated test datasource, the canonical command should be:

```bash
mvn -B -ntp clean verify
```

Until then, the explicit exclusion is a **database-contact avoidance** workaround,
not a fully safe release command:

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
9. Should Java 26 remain the required target, or should the project move to a supported
   LTS toolchain?

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
