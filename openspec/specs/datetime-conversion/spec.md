# datetime-conversion Specification

## Purpose

TBD - created by archiving change bug-localdatetime-converter-jvm-zone-leak. Update Purpose after sync.

## Requirements

### Requirement: `LocalDateTimeConverter` is JVM-zone-independent

`LocalDateTimeConverter.convertToDatabaseColumn` SHALL interpret the input `LocalDateTime` as a UTC instant — equivalent to `entityValue.toInstant(ZoneOffset.UTC)` — and SHALL produce a `Timestamp` whose `getTime()` equals that instant's epoch milliseconds, regardless of `TimeZone.getDefault()` at call time.

`LocalDateTimeConverter.convertToEntityAttribute` SHALL interpret the input `Timestamp` as a UTC instant and SHALL produce a `LocalDateTime` whose fields match that instant when rendered in UTC — equivalent to `LocalDateTime.ofInstant(dbValue.toInstant(), ZoneOffset.UTC)` — regardless of `TimeZone.getDefault()` at call time.

The converter SHALL NOT call `Timestamp.valueOf(LocalDateTime)`, `Timestamp.toLocalDateTime()`, `LocalDateTime.now()` (without an explicit `Clock` or `ZoneId` argument), or any other `java.time` / `java.sql` API whose behavior depends on `TimeZone.getDefault()` or `ZoneId.systemDefault()`.

The converter SHALL preserve `LocalDateTime` fields at microsecond precision: the persisted `Timestamp` corresponds to the input value truncated to `ChronoUnit.MICROS`, and round-tripping a value through both methods yields the input value truncated to `ChronoUnit.MICROS`.

`null` inputs SHALL pass through unchanged: `convertToDatabaseColumn(null)` returns `null` and `convertToEntityAttribute(null)` returns `null`.

#### Scenario: Persisted instant matches UTC interpretation regardless of JVM zone

- **GIVEN** a `LocalDateTime` `v` of `2026-04-29T12:00:00`
- **AND** `TimeZone.getDefault()` set to any IANA zone (UTC, Europe/Vienna, America/Los_Angeles, Asia/Tokyo)
- **WHEN** `convertToDatabaseColumn(v)` is called
- **THEN** the returned `Timestamp.getTime()` SHALL equal `v.toInstant(ZoneOffset.UTC).toEpochMilli()`

#### Scenario: Read value matches UTC interpretation regardless of JVM zone

- **GIVEN** a `Timestamp` `ts` constructed as `Timestamp.from(v.toInstant(ZoneOffset.UTC))` for some `LocalDateTime` `v`
- **AND** `TimeZone.getDefault()` set to any IANA zone (UTC, Europe/Vienna, America/Los_Angeles, Asia/Tokyo)
- **WHEN** `convertToEntityAttribute(ts)` is called
- **THEN** the returned `LocalDateTime` SHALL equal `v` (truncated to microseconds)

#### Scenario: Round-trip is independent of JVM default zone

- **GIVEN** a `LocalDateTime` `v` representing a UTC instant (e.g. produced by `DateUtils.nowUtc()`)
- **WHEN** `convertToDatabaseColumn(v)` is called with `TimeZone.getDefault()` set to zone A
- **AND** `convertToEntityAttribute` is called on the result with `TimeZone.getDefault()` set to zone B (possibly different from A)
- **THEN** the final `LocalDateTime` SHALL equal `v.truncatedTo(ChronoUnit.MICROS)`

#### Scenario: Microsecond precision is preserved

- **GIVEN** a `LocalDateTime` `v` whose nanosecond field has sub-microsecond precision
- **WHEN** `v` is round-tripped through `convertToDatabaseColumn` and `convertToEntityAttribute`
- **THEN** the result SHALL equal `v.truncatedTo(ChronoUnit.MICROS)`

#### Scenario: Null inputs pass through

- **WHEN** `convertToDatabaseColumn(null)` is called
- **THEN** the result SHALL be `null`
- **AND WHEN** `convertToEntityAttribute(null)` is called
- **THEN** the result SHALL be `null`

#### Scenario: Forbidden APIs are not used

- **GIVEN** a static inspection of `LocalDateTimeConverter`
- **THEN** the source SHALL NOT call `Timestamp.valueOf(LocalDateTime)`, `Timestamp.toLocalDateTime()`, `LocalDateTime.now()` without an explicit `Clock` or `ZoneId` argument, or any other API whose behavior depends on `TimeZone.getDefault()` / `ZoneId.systemDefault()`
