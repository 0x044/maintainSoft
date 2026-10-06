# MaintainSoft

A backend-only maintenance-operations API: departments, machines, machine statuses,
spare-parts inventory, and a repair lifecycle. Spring Boot 4 on Java 25 with PostgreSQL.

## Prerequisites

- JDK 25 (the build fails fast on any other version)
- The PostgreSQL instance the application uses. No Docker is involved

Use the committed Maven Wrapper so the Maven version is pinned:

```bash
./mvnw -version
```

## Build and test

```bash
./mvnw -B -ntp clean verify
```

The suite connects to the same PostgreSQL instance as the application, named in
`spring.datasource.url`. Most tests roll back their writes, so they leave nothing
behind; the concurrency and deactivation tests commit and clean up after themselves.

To run against a different database, override the three datasource properties:

```bash
./mvnw -B -ntp clean verify \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/maintainsoft_test \
  -Dspring.datasource.username=... \
  -Dspring.datasource.password=...
```

## Running the application

### 1. Generate a signing key pair

The JWT signing keys are not committed and are not packaged. Generate a local pair:

```bash
./scripts/generate-jwt-keys.sh
```

This writes `config/private.key` and `config/public.key`, which are git-ignored. The
script refuses to overwrite an existing pair so a rotation is always deliberate.

### 2. Supply secrets

The datasource password is encrypted with Jasypt. Provide the master password through
the environment:

```bash
export JASYPT_ENCRYPTOR_PASSWORD='<secret>'
```

Point the datasource at your own database by overriding the properties in
`src/main/resources/application.properties` (or, preferably, through environment
variables such as `SPRING_DATASOURCE_URL`):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/maintainsoft
spring.datasource.username=...
spring.datasource.password=ENC(...)
```

### 3. Start

```bash
./mvnw -B -ntp spring-boot:run
```

The service listens on port `6969`. Swagger UI is at `/swagger/ui` and Actuator at
`/actuator`; both are restricted in the `prod` profile.

### Optional: first manager

Bootstrap seeding is disabled by default. To create the first manager and department,
enable it and supply every value explicitly — nothing is defaulted or predictable:

```bash
export APP_BOOTSTRAP_ENABLED=true
export APP_BOOTSTRAP_DEPARTMENT_NAME='Operations'
export APP_BOOTSTRAP_POC_NAME='...'
export APP_BOOTSTRAP_POC_NUMBER='...'
export APP_BOOTSTRAP_MANAGER_NAME='...'
export APP_BOOTSTRAP_MANAGER_EMAIL='...'
export APP_BOOTSTRAP_MANAGER_PHONE='...'
export APP_BOOTSTRAP_MANAGER_PASSWORD='...'
```

## Key configuration

| Property | Default | Purpose |
| --- | --- | --- |
| `app.security.rsa.private-key` | `file:./config/private.key` | PEM PKCS#8 signing key |
| `app.security.rsa.public-key` | `file:./config/public.key` | PEM X.509 verification key |
| `app.bootstrap.enabled` | `false` | First-manager seeding |
| `app.rate-limit.auth.login-permits` | `10` | Login attempts per client per period |
| `app.rate-limit.auth.login-period-seconds` | `60` | Login window length |
| `app.rate-limit.auth.refresh-permits` | `30` | Refresh attempts per client per period |
| `app.rate-limit.auth.refresh-period-seconds` | `60` | Refresh window length |

Login and refresh are throttled per client address and return `429` with the standard
error body once the budget is spent. The limit is applied before the controller, so a
rejected attempt performs no password hashing and writes nothing.

> Behind a reverse proxy, **strip `X-Forwarded-For` at the edge**. The limiter trusts
> that header to identify the caller, so a forged value would let an attacker rotate
> addresses and bypass the limit.

Both key properties accept any Spring resource location, so a deployment can use a
mounted secret instead of a file on disk:

```properties
app.security.rsa.private-key=file:/run/secrets/jwt-private-key.pem
app.security.rsa.public-key=file:/run/secrets/jwt-public-key.pem
```

The application refuses to start if a configured key is missing or malformed.

## Database migrations

Flyway applies `src/main/resources/db/migration` on startup; JPA runs with
`ddl-auto=validate` so the schema is only ever changed by a migration. Never edit an
already-applied migration — add a new one.

Flyway validates the checksum of every applied migration against the file on disk at
startup, so editing a migration that has already run will stop the application from
booting. If that happens, decide deliberately between reverting the file, baselining, or
adding a new migration — do not run `repair` to silence the mismatch. `MigrationStateTest`
reports the applied chain and flags any baselined rows.

## Tests

| Location | Scope |
| --- | --- |
| `src/test/java/com/maintainsoft/integration` | Rollback-only PostgreSQL coverage: repair, stock, costs, archive, audit, constraints, migrations |
| `src/test/java/com/maintainsoft/security` | HTTP authorization, CORS, JWT decoding, key-packaging guard |
| `src/test/java/com/maintainsoft/service` | Mockito unit tests |
| `src/test/java/com/maintainsoft/testsupport` | Throwaway key pair and security-context helpers |

Run one class with:

```bash
./mvnw -B -ntp test -Dtest=RepositoryConstraintIntegrationTest
```

## Adding an endpoint

Authorization is enforced **twice**, and both layers must allow the operation.

1. **Route rules.** `SecurityConfig` denies by default: a new route is refused until
   someone deliberately maps it, so an endpoint cannot silently become available to
   every authenticated caller. `GET /api/v1/**` is already open to any authenticated
   user; every mutating method needs an explicit rule.
2. **Service rules.** Mutating service methods carry `@PreAuthorize`, so the rule
   travels with the operation. A new controller, scheduled task, or internal call
   cannot bypass the route rules. Manager-only operations use `hasRole('MANAGER')`;
   shared ones use `hasAnyRole('MANAGER', 'SUPERVISOR')`.
3. **Tests.** Add the case to `RoleMatrixHttpTest` (the HTTP contract) and to
   `MethodSecurityTest` (the service contract). Both fail if an expected status changes.

This policy exists because two bypasses shipped unnoticed: `/api/v1/departments` was
never mapped at all, and `POST /api/v1/repairs` fell through to
`anyRequest().authenticated()`.

## Security status

Read `ROADMAP.md` before deploying anything. In particular, the RSA key pair and the
Jasypt master password were committed in earlier revisions, so those credentials must be
treated as compromised and rotated before any real deployment.
