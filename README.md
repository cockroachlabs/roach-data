# Roach Data

Collection of small Spring Boot demos using CockroachDB with common data access frameworks and ORMs.

The purpose is to showcase how CockroachDB can be used with a mainstream Enterprise Java framework
stack composed by Spring Boot, Spring Data and Spring HATEOAS. 

Data access variants include:

- [JDBC](roach-data-jdbc/README.md) - using Spring Data JDBC 
- [JDBC (plain)](roach-data-jdbc-plain/README.md) - using plain JDBC 
- [JPA](roach-data-jpa/README.md) - using Spring Data JPA with Hibernate as ORM provider 
- [JPA Orders](roach-data-jpa-orders/README.md) - using Spring Data JPA to model a simple order system 
- [jOOQ](roach-data-jooq/README.md) - using Spring Boot with jOOQ (not officially supported by spring-data) 
- [MyBatis](roach-data-mybatis/README.md) - using Spring Data MyBatis/JDBC
- [JSON](roach-data-json/README.md) - using Spring Data JPA and JSONB types with inverted indexes
- [Reactive](roach-data-reactive/README.md) - using Spring Data r2dbc with the reactive PSQL driver

The demos are independent and use a similar schema and test workload. 

Common Spring Boot features demonstrated:

- Liquibase Schema versioning
- Hikari Connection Pool
- Executable jar with embedded Jetty container
- Pagination, both manual and via Spring Data JPA 
- Transaction retries with exponential backoff using AspectJ
- Hypermedia API via Spring HATEOAS and HAL media type
- Simple HTTP client invoking commands

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
    
### JDBC demo

(Using custom JDBC URL)

    java -jar roach-data-jdbc/target/roach-data-jdbc.jar --spring.datasource.url=jdbc:postgresql://localhost:26257/roach_data?sslmode=disable

Run with contention/retries:

    java -jar roach-data-jdbc/target/roach-data-jdbc.jar --concurrency=8

### JPA demo

    java -jar roach-data-jpa/target/roach-data-jpa.jar

### JPA orders demo

    java -jar roach-data-jpa-orders/target/roach-data-jpa-orders.jar

### jOOQ demo

    java -jar roach-data-jooq/target/roach-data-jooq.jar

### MyBatis demo

    java -jar roach-data-mybatis/target/roach-data-mybatis.jar

### Reactive demo

    java -jar roach-data-reactive/target/roach-data-reactive.jar
