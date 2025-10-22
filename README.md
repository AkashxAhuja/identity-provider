# Identity Provider Platform

This repository hosts two Spring Boot microservices that together deliver device management and OAuth 2.0 style access token capabilities.

- **device-management** – manages device registration, biometric activation, and detagging workflows backed by an H2 schema that mirrors `DEVICE_MASTER` and `DEVICE_MASTER_HISTORY` tables.
- **access-token** – issues, validates, revokes JWT access tokens and publishes an OAuth 2.0 compatible JWKS endpoint suitable for downstream services that need to validate bearer tokens.

Both services are packaged as individual Gradle modules so they can be built, tested, and deployed independently.

## Prerequisites

- Java 21+
- Gradle 8.7+ (or use the Gradle wrapper once plugin artifacts are available locally)
- Required Spring Boot artifacts published to a repository reachable from this environment (for example `mavenLocal()`), because external network access is not guaranteed.

## Project layout

```
identity-provider/
├── device-management/    # Device lifecycle service
│   └── src/...
├── access-token/         # JWT issuance & introspection service
│   └── src/...
├── build.gradle.kts      # Shared Gradle configuration
└── settings.gradle.kts   # Module declarations
```

## Building

To build all modules:

```bash
gradle build
```

To build a specific microservice:

```bash
cd device-management
gradle build
```

or

```bash
cd access-token
gradle build
```

> **Tip:** When running from the repository root you can also target a single module by using Gradle's project selector, for example `gradle :device-management:bootRun`.

## Running the services locally

### Device management service

```bash
gradle :device-management:bootRun
```

- Starts on [http://localhost:8080](http://localhost:8080)
- Exposes `/devices`, `/devices/biometrics`, and `/devices/detag`
- Uses an in-memory H2 database; schema is bootstrapped from [`device-management/src/main/resources/schema.sql`](device-management/src/main/resources/schema.sql)

### Access token service

```bash
gradle :access-token:bootRun
```

- Starts on [http://localhost:8081](http://localhost:8081)
- Exposes `/oauth/token`, `/oauth/token/introspect`, `/oauth/token/revoke`, and `/.well-known/jwks.json`
- Issues RSA signed JWTs with RS256 and publishes public key material via JWKS

## Testing

Execute the full test suite:

```bash
gradle test
```

Run module-specific tests:

```bash
gradle :device-management:test
```

```bash
gradle :access-token:test
```

The device management tests exercise the complete registration → biometrics → detag lifecycle. The access token tests cover issuing a token, introspection, revocation, and JWKS discovery.

## API highlights

### Device management

| Endpoint | Description |
| --- | --- |
| `POST /devices` | Registers a device with idempotency support. Accepts `Idempotency-Key` header to avoid duplicates. |
| `POST /devices/biometrics` | Enables biometrics for an existing device, enforcing type constraints. |
| `POST /devices/detag` | Moves a device to history, revokes sessions (event hook), and returns status `I`. |

Full request/response samples are available in the controller Javadoc and the integration tests.

### Access token

| Endpoint | Description |
| --- | --- |
| `POST /oauth/token` | Implements the OAuth 2.0 `client_credentials` grant and returns a signed JWT. |
| `POST /oauth/token/introspect` | Validates a token's signature, expiration, revocation status, and returns token metadata. |
| `POST /oauth/token/revoke` | Revokes an issued token so subsequent introspection reports `active=false`. |
| `GET /oauth/.well-known/jwks.json` | Publishes the RSA public key set for downstream verification. |

Tokens are signed with RS256 using an ephemeral key pair generated at startup. Client secrets are stored using SHA-256 hashing and validated during token issuance.

## Offline and air-gapped environments

The build scripts are configured to prefer `mavenLocal()` and a local `lib/` flat directory for dependencies. If your environment restricts outbound network access you must pre-populate those locations with the Spring Boot plugin, starters, and any third-party dependencies required by the services.
