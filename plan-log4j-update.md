# Log4j Migration Plan: rdb-utils

**Layer**: 4 — update after `jre-utils`.

## Before Starting

Prompt the user for the following version numbers before making any changes:

| Variable | Question |
|----------|----------|
| `NEW_PARENT_POM_VERSION` | What is the new `parent-pom` version? |
| `NEW_JRE_UTILS_VERSION` | What is the new `jre-utils` version? |
| `OWN_NEW_VERSION` | What version should `rdb-utils` be bumped to? (currently `1.0.0`) |

## Context

Part of a migration from Log4j 1.x to Log4j 2.25.3 across all libraries. Has 7 classes using `@Slf4j` — no code changes needed (SLF4J API is unchanged). Has extended logger suppressions for Hibernate, Liquibase, and c3p0.

> **IMPORTANT for execution**: This plan should be executed by actually making the file changes described below — create the new `log4j2.xml` / `log4j2-test.xml` files with the content provided, and delete the old `log4j.properties` files. Do not leave the config migration as a manual step.

## Current State

- **Artifact**: `info.unterrainer.commons:rdb-utils`
- **Parent**: `parent-pom:1.0.1`
- **In-house dependencies**:
  - `jre-utils:1.0.1` — bump to new version
- **log4j.properties**: YES (main + test)
- **@Slf4j usage**: 7 classes

### Current `log4j.properties` content (main):
```properties
log4j.rootLogger=DEBUG, A1
log4j.appender.A1=org.apache.log4j.ConsoleAppender
log4j.appender.A1.layout=org.apache.log4j.PatternLayout
log4j.appender.A1.layout.ConversionPattern=%-4r [%t] %-5p %c %x - %m%n
log4j.logger.io.netty=WARN
log4j.logger.org.eclipse.milo=WARN
log4j.logger.org.hibernate=WARN
log4j.logger.com.mchange.v2.log.MLog=WARN
log4j.logger.org.jboss.logging=WARN
log4j.logger.liquibase.servicelocator=WARN
log4j.logger.liquibase.resource=WARN
log4j.logger.com.mchange.v2=WARN
```

**Note**: No `charset=UTF-8` on the layout (unlike most others).

## Steps

### 1. Update parent version in `pom.xml`

Change the parent version (line 7) to the new parent-pom version.

### 2. Update in-house dependency versions in `pom.xml`

```xml
<dependency>
    <groupId>info.unterrainer.commons</groupId>
    <artifactId>jre-utils</artifactId>
    <version>NEW_JRE_UTILS_VERSION</version>
</dependency>
```

### 3. Bump own version

Increment `<version>` (line 11, currently `1.0.0`).

### 4. Create `src/main/resources/log4j2.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%-4r [%t] %-5p %c %x - %m%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Logger name="io.netty" level="WARN"/>
        <Logger name="org.eclipse.milo" level="WARN"/>
        <Logger name="org.hibernate" level="WARN"/>
        <Logger name="com.mchange.v2.log.MLog" level="WARN"/>
        <Logger name="org.jboss.logging" level="WARN"/>
        <Logger name="liquibase.servicelocator" level="WARN"/>
        <Logger name="liquibase.resource" level="WARN"/>
        <Logger name="com.mchange.v2" level="WARN"/>
        <Root level="DEBUG">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

### 5. Create `src/test/resources/log4j2-test.xml`

Same content as above.

### 6. Delete old config files

- Delete `src/main/resources/log4j.properties`
- Delete `src/test/resources/log4j.properties`

### 7. Build, test, install

```bash
mvn clean install
```

## Files Changed

| File | Action |
|------|--------|
| `pom.xml` | Update parent version, update jre-utils version, bump own version |
| `src/main/resources/log4j2.xml` | Create |
| `src/test/resources/log4j2-test.xml` | Create |
| `src/main/resources/log4j.properties` | Delete |
| `src/test/resources/log4j.properties` | Delete |
