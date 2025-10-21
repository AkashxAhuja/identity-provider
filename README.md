# Identity Provider Device Management Service

This project implements a Spring Boot microservice that manages mobile devices for customers. It exposes three REST endpoints to register devices, enable biometrics, and detag devices while persisting data in an in-memory H2 database that mimics the provided `DEVICE_MASTER` schema.

## Prerequisites

- Java 17+
- Gradle 8.14+ (or use the provided Gradle wrapper once plugin artifacts are available)

## Running locally

```bash
gradle bootRun
```

The application starts on `http://localhost:8080` and automatically creates the schema defined in `schema.sql`. If you are
operating in an offline environment, make sure the Spring Boot Gradle plugin and dependencies are published to a repository
declared in `build.gradle.kts` (for example `mavenLocal()`).

## Running tests

```bash
gradle test
```

> **Note:** In restricted environments you may need to publish the Spring Boot Gradle plugin and required artifacts to a
> repository available to the build (e.g., a corporate Artifactory mirror or your local Maven cache) before this command can
> succeed.

## API overview

### 1. Register a device

`POST /devices`

Headers:

- `Content-Type: application/json`
- `Idempotency-Key` *(optional but recommended)*

Request body:

```json
{
  "cid": "C12345678",
  "deviceOs": "ANDROID",
  "deviceOsVer": "14",
  "model": "Pixel 8",
  "appVersion": "1.4.3",
  "lang": "en",
  "imeNo": "352011119999999",
  "deviceOsId": "ab12cd...ef",
  "userAgent": "MyApp/1.4.3 Android",
  "isTouchEnabled": true,
  "biometricType": "NONE",
  "deviceNickName": "Akash's Pixel",
  "attestationProof": "BASE64..."
}
```

- Returns **201 Created** when a new row is inserted.
- Returns **200 OK** when a duplicate is detected via the `Idempotency-Key` cache or existing device fingerprint (CID + IME NO / OS ID / MODEL & OS VER).

### 2. Enable biometrics

`POST /devices/biometrics`

```json
{
  "cid": "C12345678",
  "deviceId": "7c1a3c34-6f54-4d8a-bb2a-4f3f6e1a4c1e",
  "biometricType": "FINGERPRINT",
  "isTouchEnabled": true,
  "attestationProof": "BASE64..."
}
```

- Returns **200 OK** with the updated device projection.
- Returns **404** if the device does not belong to the customer.
- Returns **409** if the device was registered with a different biometric type.

### 3. Detag a device

`POST /devices/detag`

```json
{
  "cid": "C12345678",
  "deviceId": "7c1a3c34-6f54-4d8a-bb2a-4f3f6e1a4c1e",
  "reason": "user request",
  "revokeSessions": true,
  "hardDelete": false
}
```

- Copies the device to `device_master_history`, clears volatile fields, marks the record inactive (`status = I`), and optionally deletes the active record.
- Returns **200 OK** with the final device projection.

## Database

The database schema is created automatically via [`src/main/resources/schema.sql`](src/main/resources/schema.sql) and models the supplied table definitions for `device_master`, `device_master_history`, and an additional table to persist idempotency responses.

You can inspect the H2 console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:identitydb`).
