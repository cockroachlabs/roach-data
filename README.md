# Roach Demo Data

Collection of small Spring Boot demos using CockroachDB with common data access frameworks and ORMs.
The purpose is to showcase how CockroachDB can be used with a mainstream Enterprise Java framework
stack composed by Spring Boot, Spring Data and Spring HATEOAS. 

Data access variants include:

- [JDBC](jdbc/README.md) - using Spring Data JDBC 
- [JPA](jpa/README.md) - using Spring Data JPA with Hibernate as ORM provider 
- [jOOQ](jooq/README.md) - using Spring Boot with jOOQ (not officially supported by spring-data) 
- [MyBatis](mybatis/README.md) - using Spring Data MyBatis/JDBC

All demos are independent and use the same schema and test workload. 

Common Spring Boot features in all demos:

- Liquibase Schema versioning
- Hikari Connection Pool
- Executable jar with embedded Jetty container
- Pagination, both manual and via Spring Data JPA 
- Transaction retries with exponential backoff using AspectJ
- Hypermedia API via Spring HATEOAS and HAL media type
- Simple HTTP client invoking commands

The most documented demo is the JDBC version. It also includes an extra aspect for setting
transaction attributes including time travel / follower reads.

## Prerequisites

- JDK8+ with 1.8 language level (OpenJDK compatible)
- Maven 3+ (wrapper provided)
- CockroachDB with a database named `roach_demo_data` 

## Building

    ./mvnw clean install

See each respective module for more details.

## Running 

All demos do the same thing which is to run through a series of concurrent account
transfer requests. The requests are being intentionally submitted in such a way 
it will cause lock contention in the database and thereby trigger aborts and retry's. 
That's why the demo starts with a wall of warning messages, which is expected.
It ends with the message:

    "All client workers finished but server keeps running. Have a nice day!" 

The service remains running after the test is complete and can be access via: 

- http://localhost:8080

You could use something like Postman to send requests to the API on your own.

A custom database URL is specified with a config override:

    --spring.datasource.url=jdbc:postgresql://192.168.1.99:26257/roach_demo_data?sslmode=disable
    
### JDBC demo

    java -jar jdbc/target/roach-demo-data.jar 

### JPA demo

    java -jar jpa/target/roach-demo-data.jar

### jOOQ demo

    java -jar jooq/target/roach-demo-data.jar

### MyBatis demo

    java -jar mybatis/target/roach-demo-data.jar
