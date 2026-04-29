## Context

`LocalDateTimeConverter` is the JPA `AttributeConverter` that all consumers of `rdb-utils` rely on to map `java.time.LocalDateTime` entity fields to `java.sql.Timestamp` columns. The current implementation uses `Timestamp.valueOf(LocalDateTime)` and `Timestamp.toLocalDateTime()`, both of which interpret/format the wall-clock value through `TimeZone.getDefault()`.

The contract enforced by upstream code (notably `info.unterrainer.commons.jreutils.DateUtils.nowUtc()` and the `http-server` DAOs that populate `createdOn` / `editedOn`) is: **the `LocalDateTime` handed to JPA already represents a UTC instant**. The converter is the persistence-layer enforcement of that contract; JVM-zone math here breaks it.

The bug surfaced in `overmind-server` deployed with `TZ=Europe/Vienna`: `lastTimeOnline` writes drifted by hours, with the actual offset compounded by Hibernate's `hibernate.jdbc.time_zone=UTC` and the MariaDB connector's session-TZ behavior. Removing `TZ` masked the corruption but caused a separate cron-firing regression in the consumer. The fix needs to land at the `rdb-utils` layer so consumers can keep `TZ` set to wall-clock zones without persistence corruption.

The existing unit test does a self-consistent round-trip (`Timestamp.valueOf` → `Timestamp.toLocalDateTime`), which uses the same JVM zone for both directions and therefore cannot detect zone-dependence. The integration test has the same blind spot.

## Goals / Non-Goals

**Goals:**
- Make `LocalDateTimeConverter` produce identical `Timestamp` bytes regardless of `TimeZone.getDefault()`, treating the input `LocalDateTime` as a UTC instant.
- Make `convertToEntityAttribute` symmetric: read the `Timestamp` as a UTC instant and return the matching zone-naive `LocalDateTime`.
- Add tests that would have caught the original bug — i.e. that pin a non-UTC JVM zone before exercising both directions.
- Preserve the existing MICRO precision truncation (the existing test asserts MICRO-truncated equivalence; consumers currently observe that precision contract).

**Non-Goals:**
- Migrating existing rows that were persisted under the buggy implementation. Operators with a zone-dependent deploy who relied on the corruption are out of scope; the spec is the authoritative contract going forward.
- Changing the public type signature `AttributeConverter<LocalDateTime, Timestamp>` or adding new converters (e.g. `Instant`, `ZonedDateTime`).
- Coordinating downstream version bumps in `http-server` and `overmind-server` — those are separate changes in their own repos.
- Refactoring other converters in the package (`LocalDateConverter`, etc.). Those should be audited in a follow-up but are not the trigger for this bug.

## Decisions

### Decision 1: Use `ZoneOffset.UTC` explicitly, not `ZoneId.systemDefault()` or `ZoneId.of("UTC")`

```java
return Timestamp.from(entityValue.truncatedTo(ChronoUnit.MICROS).toInstant(ZoneOffset.UTC));
```

**Why**: `ZoneOffset.UTC` is a constant; it cannot be reconfigured at runtime, never crosses a DST boundary, and has no zone-rules lookup. `ZoneId.of("UTC")` is equivalent at runtime but pulls in the `ZoneRules` machinery for no benefit. `ZoneId.systemDefault()` is exactly the bug we are fixing.

**Alternatives considered**:
- *Switching the entity type to `Instant`*: Cleanest semantically but a breaking API change for every consumer's JPA entity. Rejected.
- *Configuring Hibernate's `hibernate.jdbc.time_zone=UTC` and removing this converter*: Hibernate's setting only governs how the driver binds parameters; it does not affect a custom `AttributeConverter` running upstream of the binding step. Removing the converter changes the column type contract. Rejected.

### Decision 2: Keep MICRO truncation

The current implementation calls `timestamp.setNanos(entityValue.truncatedTo(ChronoUnit.MICROS).getNano())`, and the existing unit test asserts MICRO-truncated equivalence. Consumers may have come to depend on that precision (some target DBs only store microseconds anyway).

**Why**: Preserving the truncation keeps the existing test green and avoids a silent precision change that downstream consumers cannot detect at compile time. The new write becomes:

```java
return Timestamp.from(entityValue.truncatedTo(ChronoUnit.MICROS).toInstant(ZoneOffset.UTC));
```

**Alternatives considered**:
- *Drop truncation, rely on `Timestamp.from(Instant)` to preserve nanos*: would change observable precision for any consumer storing a value with sub-MICRO nanos and would break the existing self-consistent round-trip test. Rejected.

### Decision 3: Test approach — toggle `TimeZone.getDefault()` inside the test

Use `TimeZone.setDefault(...)` / `TimeZone.setDefault(original)` in a `try`/`finally` over a list of zones (UTC, Europe/Vienna, America/Los_Angeles, Asia/Tokyo).

**Why**: This directly reproduces the production failure mode and is portable across CI environments without requiring `-Duser.timezone=...` JVM flags or container TZ env vars. It is also the only practical way to assert the same JVM-process behavior across multiple zones in one test method.

**Alternatives considered**:
- *Run the test suite twice via Maven Surefire profile with `-Duser.timezone=Europe/Vienna`*: catches the regression but adds CI matrix complexity and does not parameterize across multiple zones in one run. We may still want this for the integration test, but the unit test should self-contain the zone toggling.
- *Mock `TimeZone.getDefault()`*: requires PowerMock or a `Clock`-style injection point that the converter does not have. The class is a JPA-instantiated converter, so adding constructor injection is awkward. Rejected.

The integration test is extended to pin a non-UTC zone via `@BeforeAll` (saving and restoring `TimeZone.getDefault()`), so the actual JDBC pipeline is exercised under a realistic deploy condition at least once.

## Risks / Trade-offs

- **[Risk]** Existing deployments that have empirically calibrated their `TZ` env var around the bug see persisted timestamps shift after upgrade. **Mitigation**: documented as a behavioral fix in the change proposal; consumers must re-validate their `TZ` configuration. The handoff doc explicitly calls out the `overmind-server` `docker-compose.yml` as needing the `TZ=Europe/Vienna` revert.
- **[Risk]** `TimeZone.setDefault` in tests is process-global. If tests run in parallel within the same JVM, zone toggling could leak. **Mitigation**: JUnit 5 default execution is sequential per class; the converter test class only contains converter tests. We additionally restore the original zone in a `finally` block. If parallel test execution is enabled later, mark these tests `@Execution(SAME_THREAD)`.
- **[Risk]** Other converters in `info.unterrainer.commons.rdbutils.converters` may have analogous bugs (e.g. `LocalDateConverter`, `LocalTimeConverter`). **Mitigation**: out of scope here, but the new spec's requirement on JVM-zone-independence sets a precedent that future audits can apply uniformly.
- **[Trade-off]** The MICRO-truncation behavior is preserved silently rather than documented as part of the new spec. We accept this so the spec change stays minimal and backward-compatible; precision can be tightened in a separate change if a consumer ever needs full-nano fidelity.

## Migration Plan

1. Land the converter rewrite, the unit test additions, and the integration-test zone-pin in one commit on `develop`.
2. Run `mvn test` locally with the default JVM zone, then again with `-Duser.timezone=Europe/Vienna`, to confirm both pass.
3. Cut a `rdb-utils` release.
4. Downstream (separate changes in their own repos):
   - Bump `rdb-utils` version in `info.unterrainer.commons:http-server`, release.
   - Bump `http-server` version (transitively pulling the fix) in `info.unterrainer.server:overmind-server`.
   - Restore `TZ=Europe/Vienna` in `overmind-server`'s `deploy/docker-compose.yml`.
   - Verify a `lastTimeOnline` write end-to-end against the production DB.

**Rollback**: revert the converter file. Tests revert with it. No DB schema change is involved, so rollback is purely code-level. Rows written between the fix and a rollback will look identical to rows written before the bug existed (i.e. correct UTC instants), so rollback does not corrupt them — it just resumes producing zone-dependent writes.
