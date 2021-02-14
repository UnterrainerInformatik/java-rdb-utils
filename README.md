![GitHub forks](https://img.shields.io/github/forks/UnterrainerInformatik/java-rdb-utils?style=social) ![GitHub stars](https://img.shields.io/github/stars/UnterrainerInformatik/java-rdb-utils?style=social) ![GitHub repo size](https://img.shields.io/github/repo-size/UnterrainerInformatik/java-rdb-utils) [![GitHub issues](https://img.shields.io/github/issues/UnterrainerInformatik/java-rdb-utils)](https://github.com/UnterrainerInformatik/java-rdb-utils/issues)

[![license](https://img.shields.io/github/license/unterrainerinformatik/FiniteStateMachine.svg?maxAge=2592000)](http://unlicense.org) [![Travis-build](https://travis-ci.org/UnterrainerInformatik/java-rdb-utils.svg?branch=master)](https://travis-ci.org/github/UnterrainerInformatik/java-rdb-utils) [![Maven Central](https://img.shields.io/maven-central/v/info.unterrainer.commons/rdb-utils)](https://search.maven.org/artifact/info.unterrainer.commons/rdb-utils) [![Twitter Follow](https://img.shields.io/twitter/follow/throbax.svg?style=social&label=Follow&maxAge=2592000)](https://twitter.com/throbax)




# rdb-utils

A library to help with accessing and managing JAVA-persistence-layer enabled relational databases.

## RdbConfiguration

This is a class, that gathers the necessary configuration information to run a database-connection. It tries to get this information from environment variables and, if those are not present, provides default values.

The environment variables are:

```bash
DB_DRIVER="mariadb"
DB_SERVER="10.10.196.4"
DB_PORT="3306"
DB_NAME="test"
DB_USER="test"
DB_PASSWORD="test"
```



## RdbUtils

Open EntityManagerFactory and shutdownhook

Liquibase



## LocalDateTime and ZonedDateTime converter

This library also provides two two-way converters for LocalDateTime and ZonedDateTime to SQL and back.

### Usage

```java

```



## Transactions

