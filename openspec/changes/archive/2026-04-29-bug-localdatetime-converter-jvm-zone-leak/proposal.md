## Why

`LocalDateTimeConverter` corrupts persisted timestamps when the JVM default timezone is not UTC. Both `Timestamp.valueOf(LocalDateTime)` (write path) and `Timestamp.toLocalDateTime()` (read path) anchor to `TimeZone.getDefault()`, so the same code persists different bytes depending on the deployment's `TZ` env var. Consumers (e.g. `http-server` DAOs, `overmind-server`) hand the converter UTC-anchored `LocalDateTime` values via `DateUtils.nowUtc()` and rely on the converter to persist that instant verbatim — the current implementation breaks that contract and has been observed corrupting `lastTimeOnline` by hours in a `TZ=Europe/Vienna` deploy.

## What Changes

- Rewrite `LocalDateTimeConverter.convertToDatabaseColumn` to anchor the input `LocalDateTime` to `ZoneOffset.UTC` via `Timestamp.from(entityValue.truncatedTo(ChronoUnit.MICROS).toInstant(ZoneOffset.UTC))`, removing the `Timestamp.valueOf(LocalDateTime)` call.
- Rewrite `LocalDateTimeConverter.convertToEntityAttribute` to read the `Timestamp` as a UTC instant via `LocalDateTime.ofInstant(dbValue.toInstant(), ZoneOffset.UTC)`, removing the `Timestamp.toLocalDateTime()` call.
- Add zone-pinning unit tests to `LocalDateTimeConverterTests` that flip `TimeZone.getDefault()` across UTC, Europe/Vienna, America/Los_Angeles, and Asia/Tokyo, asserting both directions are JVM-zone-independent.
- Extend `LocalDateTimeConverterIntegrationTests` to pin a non-UTC JVM zone before exercising the JDBC pipeline.
- **BREAKING (semantic, not API)**: deployments running on a non-UTC JVM that have been compensating for the converter's drift (e.g. by mutating their `TZ` env var) will see the persisted instant change. The new behavior matches the documented contract that values from `DateUtils.nowUtc()` are stored as UTC instants.

## Capabilities

### New Capabilities
- `datetime-conversion`: JPA `AttributeConverter` implementations between `java.time` types and JDBC types. The first requirement covers `LocalDateTime` ↔ `Timestamp` zone-stability.

### Modified Capabilities
- (none — this repo has no existing specs)

## Impact

- **Code**: `src/main/java/info/unterrainer/commons/rdbutils/converters/LocalDateTimeConverter.java` (rewrite); `src/test/java/info/unterrainer/commons/rdbutils/LocalDateTimeConverterTests.java` (additions); `src/test/java/info/unterrainer/commons/rdbutils/LocalDateTimeConverterIntegrationTests.java` (zone-pin extension).
- **Public API**: unchanged — same class name, package, and `AttributeConverter<LocalDateTime, Timestamp>` signature.
- **Persistence semantics**: the byte stored in the DB now matches `entityValue.toInstant(ZoneOffset.UTC).toEpochMilli()` regardless of `TZ`. Existing rows written under a non-UTC JVM are not migrated (out of scope for this change).
- **Downstream consumers**: `info.unterrainer.commons:http-server` and `info.unterrainer.server:overmind-server` need a version bump of `rdb-utils` to pick up the fix; `overmind-server` can then restore `TZ=Europe/Vienna` in its `docker-compose.yml`. Coordinating the bump and the `TZ` revert is tracked separately in those repos.
- **Dependencies**: none added or removed.
