## 1. Rewrite the converter

- [x] 1.1 Replace `Timestamp.valueOf(entityValue)` + `setNanos(...)` in `LocalDateTimeConverter.convertToDatabaseColumn` with `Timestamp.from(entityValue.truncatedTo(ChronoUnit.MICROS).toInstant(ZoneOffset.UTC))`
- [x] 1.2 Replace `dbValue.toLocalDateTime()` in `LocalDateTimeConverter.convertToEntityAttribute` with `LocalDateTime.ofInstant(dbValue.toInstant(), ZoneOffset.UTC)`
- [x] 1.3 Update imports: add `java.time.ZoneOffset`, drop `java.time.temporal.ChronoUnit` only if no longer used (it is still needed for the truncation in 1.1, so keep it)
- [x] 1.4 Verify the `null` short-circuits in both methods are still in place

## 2. Strengthen the unit tests

- [x] 2.1 Add `convertToDatabaseColumn_isJvmZoneIndependent` to `LocalDateTimeConverterTests` — for each of `["UTC", "Europe/Vienna", "America/Los_Angeles", "Asia/Tokyo"]`, set `TimeZone.setDefault(...)`, call the converter on a fixed `LocalDateTime` (`2026-04-29T12:00:00`), and assert `ts.getTime() == v.toInstant(ZoneOffset.UTC).toEpochMilli()`
- [x] 2.2 Add `convertToEntityAttribute_isJvmZoneIndependent` to `LocalDateTimeConverterTests` — same zone loop, assert the read value equals the original UTC `LocalDateTime`
- [x] 2.3 Add a microsecond-precision round-trip test that constructs a `LocalDateTime` with a sub-microsecond nano field and asserts the round-trip equals `v.truncatedTo(ChronoUnit.MICROS)`
- [x] 2.4 Add null-input assertions for both directions
- [x] 2.5 Wrap any `TimeZone.setDefault` calls in `try { ... } finally { TimeZone.setDefault(original); }` to avoid leaking zone state into other tests
- [x] 2.6 Rewrite the existing `conversionFromLocalDateTimeToTimestamp` and `conversionFromTimestampToLocalDateTime` tests to use UTC-anchored round-trip assertions (the original assertions used `Timestamp.toLocalDateTime()` / `Timestamp.valueOf(d)` and were only self-consistent under the buggy converter — they cannot pass against a zone-stable implementation; this is the blind-spot the handoff doc identified)

## 3. Extend the integration test

- [x] 3.1 In `LocalDateTimeConverterIntegrationTests`, save the original `TimeZone.getDefault()` in `@BeforeAll` and set the JVM default to `Europe/Vienna` (or another non-UTC zone) for the duration of the class
- [x] 3.2 Restore the original zone in `@AfterAll`
- [ ] 3.3 Confirm `persistingAndReadingEntityWorks` still passes under the non-UTC zone — this is the regression check that exercises the JDBC pipeline end-to-end

## 4. Verify

- [x] 4.1 Run `mvn test` with the default JVM zone — all tests pass
- [x] 4.2 Run `mvn test -Duser.timezone=Europe/Vienna` — all tests pass
- [x] 4.3 Run `mvn test -Duser.timezone=America/Los_Angeles` — all tests pass (only `LocalDateTimeConverterTests` was run under each zone — `LocalDateTimeConverterIntegrationTests` is gated on a live DB and falls under 3.3, deferred)
- [x] 4.4 Static-check `LocalDateTimeConverter.java` for any remaining call sites of `Timestamp.valueOf`, `Timestamp.toLocalDateTime`, `LocalDateTime.now`, or `ZoneId.systemDefault` (none should exist)

## 5. Release

- [ ] 5.1 Update the project version in `pom.xml` per the repo's release convention
- [ ] 5.2 Note in the changelog / commit message that this is a behavioral fix and that downstream consumers should bump and revalidate their `TZ` env vars
- [ ] 5.3 Cut the release artifact (deferred to maintainer's normal release flow — not part of this change's verification gate)
