# MaintainSoft

A backend-only maintenance-operations API: departments, machines, machine statuses,
spare-parts inventory, and a repair lifecycle. Spring Boot 4 on Java 25 with PostgreSQL.

## Prerequisites

- JDK 25 (the build fails fast on any other version)
- No database, no Docker, and no secrets are needed to run the tests

Use the committed Maven Wrapper so the Maven version is pinned:

```bash
./mvnw -version
```

## Build and test

```bash
./mvnw -B -ntp clean verify
```

The suite starts an embedded PostgreSQL instance on a loopback port, migrates it from
empty, and rolls back each test's writes. It never contacts the application database.

To check the migration chain against the PostgreSQL version used in deployment, point
the suite at a disposable database instead:

```bash
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/maintainsoft_test \
TEST_DATABASE_USERNAME=... \
TEST_DATABASE_PASSWORD=... \
./mvnw -B -ntp clean verify
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

Before pointing this at an existing database, inspect that database's
`flyway_schema_history` and decide deliberately between preserving V1, baselining, or
creating new migrations. Do not run `repair` to silence a checksum mismatch.

## Tests

| Location | Scope |
| --- | --- |
| `src/test/java/com/maintainsoft/integration` | Rollback-only PostgreSQL coverage: repair, stock, costs, archive, audit, constraints, migrations |
| `src/test/java/com/maintainsoft/security` | HTTP authorization, CORS, JWT decoding, key-packaging guard |
| `src/test/java/com/maintainsoft/service` | Mockito unit tests |
| `src/test/java/com/maintainsoft/testsupport` | Embedded database and throwaway key pair used by the whole suite |

Run one class with:

```bash
./mvnw -B -ntp test -Dtest=RepositoryConstraintIntegrationTest
```

## Security status

Read `ROADMAP.md` before deploying anything. In particular, the RSA key pair and the
Jasypt master password were committed in earlier revisions, so those credentials must be
treated as compromised and rotated before any real deployment.
