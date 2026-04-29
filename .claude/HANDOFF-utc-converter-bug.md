# Handoff: `LocalDateTimeConverter` is JVM-zone-dependent and corrupts timestamps in non-UTC deployments

This document is a complete brief for a fresh Claude instance working in the `rdb-utils` repo. It contains everything needed to run `/opsx:propose` and produce a sound change. The bug was discovered by a sibling investigation in the consumer project `overmind-server`; see "Provenance" at the bottom for cross-references.

## Symptom

A consumer (`overmind-server`, deployed in a Docker container with `TZ=Europe/Vienna`) observed that `LocalDateTime` columns persisted via JPA (e.g. `lastTimeOnline`) were stored with a -4h offset relative to the actual UTC time. Removing the `TZ` env var so the JVM defaults to UTC made the values correct again, but introduced a separate problem (cron expressions in the consumer fire +2h late). The consumer's writers correctly produce UTC values via `info.unterrainer.commons.jreutils.DateUtils.nowUtc()` before the entity is handed to JPA, so the corruption is downstream of the entity — i.e. inside this library.

## Root cause

`src/main/java/info/unterrainer/commons/rdbutils/converters/LocalDateTimeConverter.java`:

```java
@Converter()
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(final LocalDateTime entityValue) {
        if (entityValue == null) return null;
        Timestamp timestamp = Timestamp.valueOf(entityValue);             // ← line 17
        timestamp.setNanos(entityValue.truncatedTo(ChronoUnit.MICROS).getNano());
        return timestamp;
    }

    @Override
    public LocalDateTime convertToEntityAttribute(final Timestamp dbValue) {
        if (dbValue == null) return null;
        return dbValue.toLocalDateTime();                                  // ← line 26
    }
}
```

Both calls are JVM-default-timezone-dependent:

- **`Timestamp.valueOf(LocalDateTime)`** is documented to interpret the `LocalDateTime` "as the local date-time in the local time zone." It anchors the wall-clock value to whatever `TimeZone.getDefault()` returns at call time, producing an `Instant` that is shifted by the JVM's offset from UTC. With JVM=UTC the shift is zero (no corruption); with JVM=Europe/Vienna the shift is -2h (a `LocalDateTime` of 12:00 representing UTC becomes an instant of 10:00Z because the method reads the 12:00 as Vienna-local).
- **`Timestamp.toLocalDateTime()`** is the inverse: it formats the instant as a wall-clock value in the JVM's default zone.

The contract the consumer expects (and which the http-server library `info.unterrainer.commons:http-server` enforces by populating `createdOn` / `editedOn` via `DateUtils.nowUtc()`) is: **the `LocalDateTime` value handed to JPA already represents a UTC instant, and the converter SHALL persist that instant verbatim, regardless of JVM default timezone**. The current implementation breaks that contract.

The downstream observed corruption is -4h (not -2h) because the persistence stack has multiple zone-aware layers stacked on top of each other:
1. This converter applies the JVM zone offset (-2h with JVM=Vienna).
2. Hibernate's `hibernate.jdbc.time_zone=UTC` setting in the consumer's `persistence.xml` re-interprets the `Timestamp` produced by step 1.
3. The MariaDB Connector/J driver's session-TZ behavior may apply a further offset depending on the DB server's configured timezone.

The fix lives at step 1; downstream layers become correct as soon as the converter is zone-stable.

## Why the existing tests pass

`src/test/java/info/unterrainer/commons/rdbutils/LocalDateTimeConverterTests.java` does this round-trip:

```java
LocalDateTime d = DateUtils.nowUtc();
Timestamp ts = converter.convertToDatabaseColumn(d);
assertThat(ts.toLocalDateTime()).isEqualTo(d.truncatedTo(ChronoUnit.MICROS));
```

Both the write (`Timestamp.valueOf`) and the read (`Timestamp.toLocalDateTime`) anchor to the same JVM zone, so the round-trip is self-consistent **regardless of what that zone is**. The test cannot detect the bug because it never pins the JVM to a non-UTC zone, and it never exercises the Hibernate / JDBC binding pipeline where the extra layers stack.

The integration test (`LocalDateTimeConverterIntegrationTests.java`) likely has the same blind spot — it should be inspected and probably extended with the zone-pinning assertion described in the "Required test additions" section below.

## Required fix

```java
package info.unterrainer.commons.rdbutils.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(final LocalDateTime entityValue) {
        if (entityValue == null) return null;
        return Timestamp.from(entityValue.toInstant(ZoneOffset.UTC));
    }

    @Override
    public LocalDateTime convertToEntityAttribute(final Timestamp dbValue) {
        if (dbValue == null) return null;
        return LocalDateTime.ofInstant(dbValue.toInstant(), ZoneOffset.UTC);
    }
}
```

`ZoneOffset.UTC` makes both directions zone-stable. A `LocalDateTime` of 12:00 (representing UTC, as `DateUtils.nowUtc()` produces) anchors to instant 12:00Z regardless of `TimeZone.getDefault()`. Round-trip is preserved in any deploy.

The `setNanos(entityValue.truncatedTo(ChronoUnit.MICROS).getNano())` line in the original is dropped because `Timestamp.from(Instant)` already preserves nanos faithfully. If MICRO truncation is intentional (e.g. some DBs only store microsecond precision and the original code was rounding away precision the DB can't represent), preserve it explicitly:

```java
return Timestamp.from(entityValue.truncatedTo(ChronoUnit.MICROS).toInstant(ZoneOffset.UTC));
```

The change should match whichever shape the existing tests assert. If the test asserts MICRO-truncation equivalence, keep the truncation; if it asserts full-nano fidelity, drop it. (My read: the existing test asserts MICRO truncation via `d.truncatedTo(ChronoUnit.MICROS)`, so keep the truncation.)

## Required test additions

Add to `src/test/java/info/unterrainer/commons/rdbutils/LocalDateTimeConverterTests.java`:

```java
@Test
void convertToDatabaseColumn_isJvmZoneIndependent() {
    LocalDateTime utcValue = LocalDateTime.of(2026, 4, 29, 12, 0, 0);
    long expectedEpochMillis = utcValue.toInstant(ZoneOffset.UTC).toEpochMilli();

    TimeZone original = TimeZone.getDefault();
    try {
        for (String zoneId : List.of("UTC", "Europe/Vienna", "America/Los_Angeles", "Asia/Tokyo")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
            Timestamp ts = converter.convertToDatabaseColumn(utcValue);
            assertThat(ts.getTime())
                .as("Persisted instant must equal the UTC interpretation regardless of JVM zone (was %s)", zoneId)
                .isEqualTo(expectedEpochMillis);
        }
    } finally {
        TimeZone.setDefault(original);
    }
}

@Test
void convertToEntityAttribute_isJvmZoneIndependent() {
    LocalDateTime utcValue = LocalDateTime.of(2026, 4, 29, 12, 0, 0);
    Timestamp ts = Timestamp.from(utcValue.toInstant(ZoneOffset.UTC));

    TimeZone original = TimeZone.getDefault();
    try {
        for (String zoneId : List.of("UTC", "Europe/Vienna", "America/Los_Angeles", "Asia/Tokyo")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
            LocalDateTime read = converter.convertToEntityAttribute(ts);
            assertThat(read)
                .as("Read value must equal the original UTC value regardless of JVM zone (was %s)", zoneId)
                .isEqualTo(utcValue);
        }
    } finally {
        TimeZone.setDefault(original);
    }
}
```

These tests would have failed against the broken implementation in any non-UTC JVM and confirm the new implementation is zone-stable. The existing test (`conversionFromLocalDateTimeToTimestamp` and `conversionFromTimestampToLocalDateTime`) should keep passing unchanged — its self-consistent round-trip assertion is still valid.

## Required spec contract

The change SHOULD add a new `datetime-conversion` capability spec (or amend an existing one if `rdb-utils` already has spec coverage — check `openspec/specs/`) with this requirement:

### Requirement: `LocalDateTimeConverter` is JVM-zone-independent

`LocalDateTimeConverter.convertToDatabaseColumn` SHALL interpret the input `LocalDateTime` as a UTC instant — `Instant equivalent = entityValue.toInstant(ZoneOffset.UTC)` — and SHALL produce a `Timestamp` whose `getTime()` equals that instant's epoch milliseconds, regardless of `TimeZone.getDefault()`.

`LocalDateTimeConverter.convertToEntityAttribute` SHALL interpret the input `Timestamp` as a UTC instant and SHALL produce a `LocalDateTime` whose fields match that instant when rendered in UTC — `result == LocalDateTime.ofInstant(dbValue.toInstant(), ZoneOffset.UTC)` — regardless of `TimeZone.getDefault()`.

The converter SHALL NOT call any of `Timestamp.valueOf(LocalDateTime)`, `Timestamp.toLocalDateTime()`, or any other API whose behavior depends on `TimeZone.getDefault()` / `ZoneId.systemDefault()`. The converter is the persistence-layer enforcement of the consumer's contract that "values handed to JPA are UTC-anchored." JVM-default-zone math anywhere in this class breaks that contract.

**Rationale**: Consumers (notably `info.unterrainer.commons:http-server`) populate timestamp fields via `DateUtils.nowUtc()` before the entity reaches JPA. The DAOs in `http-server` (`JpqlCoreDao.java:55-57`, `:116-117`, `UpdateQueryBuilder.java:35-36`) all use `DateUtils.nowUtc()` for `createdOn` / `editedOn`. The converter is the last line of defense against zone leakage; it must respect the UTC contract regardless of where the JVM is running. A `Timestamp.valueOf(LocalDateTime)` call here makes deploys non-portable: the same bytes plus a different `TZ` environment variable produces different DB contents, with no warning at unit-test time because tests use the same JVM zone for write and read.

#### Scenario: Round-trip is independent of JVM default zone

- **GIVEN** a `LocalDateTime` value `v` representing a UTC instant (e.g. produced by `DateUtils.nowUtc()`)
- **WHEN** `convertToDatabaseColumn(v)` is called with `TimeZone.getDefault()` set to any IANA zone (UTC, Europe/Vienna, America/Los_Angeles, Asia/Tokyo, …)
- **AND** `convertToEntityAttribute` is called on the result with `TimeZone.getDefault()` set to any (potentially different) IANA zone
- **THEN** the final `LocalDateTime` SHALL equal `v` (or `v.truncatedTo(ChronoUnit.MICROS)` if MICRO truncation is the documented precision)

#### Scenario: Persisted instant matches UTC interpretation

- **GIVEN** a `LocalDateTime` `v` of `2026-04-29T12:00:00`
- **WHEN** `convertToDatabaseColumn(v)` is called
- **THEN** the returned `Timestamp.getTime()` SHALL equal `v.toInstant(ZoneOffset.UTC).toEpochMilli()`
- **AND** this SHALL hold for every value of `TimeZone.getDefault()`

#### Scenario: Forbidden APIs are not used

- **GIVEN** a static analysis pass over `LocalDateTimeConverter`
- **THEN** the source SHALL NOT call `Timestamp.valueOf(LocalDateTime)`, `Timestamp.toLocalDateTime()`, `LocalDateTime.now()` (without an explicit `Clock` or `ZoneId` argument), or any other `java.time` / `java.sql` API whose Javadoc lists `TimeZone.getDefault()` or `ZoneId.systemDefault()` as a dependency

## Downstream impact

After the fix lands and a new version of `rdb-utils` is released, downstream consumers must bump and redeploy:

- `info.unterrainer.commons:http-server` — depends on `rdb-utils` for `BasicJpa` and the converter. A version bump is the cleanest path.
- `info.unterrainer.server:overmind-server` — depends on both `rdb-utils` and `http-server`; transitively picks up the converter fix once one of them bumps. After the bump, `overmind-server`'s deploy can restore `TZ=Europe/Vienna` in `deploy/docker-compose.yml` (currently commented out as a workaround for this exact bug). Restoring `TZ` also fixes a separate cron-firing regression in `PlanHandler` that's currently being triaged in `overmind-server`'s `bug-plan-restore-action-not-firing` change.

The change in `rdb-utils` is therefore the unblocking fix for two downstream bugs at once.

## Verification (after the fix lands)

1. **Unit tests** — both the new zone-pinning tests and the existing self-consistent round-trip tests pass in `mvn test`. Run with `-Duser.timezone=Europe/Vienna` to confirm one zone non-trivially; run with the default to confirm UTC.
2. **Integration test** — extend `LocalDateTimeConverterIntegrationTests` to also pin a non-UTC JVM zone before exercising the actual JDBC pipeline. This is the test that would have caught the regression in production conditions.
3. **Downstream smoke** — bump `rdb-utils` version in `http-server` and `overmind-server`, redeploy `overmind-server` with `TZ=Europe/Vienna` restored in `docker-compose.yml`, observe a `lastTimeOnline` write and confirm the persisted DB value (queried directly with `mysql` / `mariadb` CLI from a UTC-set session) matches the actual UTC instant.

## Provenance

This bug was diagnosed during investigation of a separate issue in `overmind-server` (`bug-plan-restore-action-not-firing`, currently in `openspec/changes/`). The investigation chain was:

1. User reported that scheduled "restore to full brightness" lighting plans no longer fire on time.
2. Initial hypothesis: the `91e368b update timezone` deploy-config commit (which commented out `TZ=Europe/Vienna`) had broken cron-tick evaluation in `PlanHandler.sortAndRemoveCrons`, because `ZonedDateTime.now()` now returns a UTC instant.
3. User clarified that `TZ=Europe/Vienna` had been removed because of an independent observation: with `TZ=Europe/Vienna`, the `lastTimeOnline` field was being persisted with a -4h offset relative to actual UTC.
4. Tracing the persistence pipeline showed that all timestamp writers in the consumer use `DateUtils.nowUtc()` before the entity reaches JPA — the `LocalDateTime` values produced are UTC-anchored zone-naive representations.
5. Inspection of `LocalDateTimeConverter` revealed the JVM-zone-dependent `Timestamp.valueOf(LocalDateTime)` and `Timestamp.toLocalDateTime()` calls — the persistence-layer corruption was in `rdb-utils`, not in the consumer or in Hibernate / the JDBC driver.

The fix in `rdb-utils` allows the consumer to restore `TZ=Europe/Vienna`, which fixes both:
- The cron-tick zone-mismatch (so dim/restore plans fire at the operator's wall-clock times).
- The original `lastTimeOnline` corruption motivation that prompted the `TZ` removal.

No `PlanHandler` change is needed in the consumer once `rdb-utils` is fixed; the consumer's existing `ZonedDateTime.now()` call becomes correct again as soon as `TZ` is restored.

## Suggested change name

`bug-localdatetime-converter-jvm-zone-leak` (kebab-case, descriptive, follows the consumer's naming convention for bug changes).

## What `/opsx:propose` should produce

A standard openspec change with `proposal.md`, `design.md`, `tasks.md`, and a `specs/datetime-conversion/spec.md` (or whatever capability name fits this repo's existing spec layout — check `openspec/specs/` first; if no specs exist yet, this change can introduce the first one). The proposal should center on the converter fix, the existing test's blind spot, the new zone-pinning test, and the downstream-bump-and-redeploy chain. Hold all consumer-side changes (the `TZ` revert in `overmind-server`) as out-of-scope follow-ups; this change is library-only.
