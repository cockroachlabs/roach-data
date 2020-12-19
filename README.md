# Roach Demo Data

Collection of small Spring Boot demos using CockroachDB with common data access frameworks and ORMs.
The purpose is to showcase how CockroachDB can be used with a mainstream Enterprise Java framework
stack composed by Spring Boot, Spring Data and Spring HATEOAS. 

Data access variants include:

- [JDBC](roach-data-jdbc/README.md) - using Spring Data JDBC 
- [JDBC (plain)](roach-data-jdbc-plain/README.md) - using plain JDBC 
- [JPA](roach-data-jpa/README.md) - using Spring Data JPA with Hibernate as ORM provider 
- [jOOQ](roach-data-jooq/README.md) - using Spring Boot with jOOQ (not officially supported by spring-data) 
- [MyBatis](roach-data-mybatis/README.md) - using Spring Data MyBatis/JDBC
- [JSON](roach-data-json/README.md) - using Spring Data JPA and JSONB types with inverted indexes

Most demos are independent and use the same schema and test workload. 

Common Spring Boot features demonstrated:

- Liquibase Schema versioning
- Hikari Connection Pool
- Executable jar with embedded Jetty container
- Pagination, both manual and via Spring Data JPA 
- Transaction retries with exponential backoff using AspectJ
- Hypermedia API via Spring HATEOAS and HAL media type
- Simple HTTP client invoking commands

The most documented demo is the JDBC version. It includes an extra aspect for setting transaction 
attributes such timeouts and read-only hints. 

## Prerequisites

- JDK8+ with 1.8 language level (OpenJDK compatible)
- Maven 3+ (wrapper provided)
- CockroachDB with a database named `roach_data` 

## Building

    ./mvnw clean install

See each respective module for more details.

## Running 

All demos do the same thing, which is to run through a series of concurrent account
transfer requests. The requests are being intentionally submitted in a way that 
will cause lock contention in the database and trigger aborts and retry's. 

By default, the contention level is zero (serial execution) so you won't see any errors. 
To observe these errors, pass a number (>1) to the command line representing the thread count. 
Then you will see transaction conflicts and retries on the server side until the demo 
settles with an end message:

    "All client workers finished but server keeps running. Have a nice day!"

The service remains running after the test is complete and can be access via: 

- http://localhost:9090

You could use something like Postman to send requests to the API on your own.

A custom database URL is specified with a config override:

    --spring.datasource.url=jdbc:postgresql://192.168.1.123:26257/roach_data?sslmode=disable
    
### JDBC demo

    java -jar roach-data-jdbc/target/roach-data-jdbc.jar

with contention:

    java -jar roach-data-jdbc/target/roach-data-jdbc.jar 8

### JPA demo

    java -jar roach-data-jpa/target/roach-data-jpa.jar

### jOOQ demo

    java -jar roach-data-jooq/target/roach-data-jooq.jar

### MyBatis demo

    java -jar roach-data-mybatis/target/roach-data-mybatis.jar
