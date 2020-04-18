# Roach Demo Data :: jOOQ

Spring Boot Demo using CockroachDB with jOOQ.

## Generate jOOQ classes

First create a DB schema:

    create table account
    (
        id      int            not null primary key,
        balance numeric(19, 2) not null,
        name    varchar(128)   not null,
        type    varchar(25)    not null
    );

Then generate the code by activating a Maven profile called generate:

    mvn -P generate clean install
   
(Note: this will fail with an error when using CRDB even if classes are generated correctly)    

Finally drop the table:

    drop table account cascade;
    
## Run

    java -jar target/roach-demo-data.jar

Run with custom db URL:

    java -jar target/roach-demo-data.jar --spring.datasource.url=jdbc:postgresql://localhost:26257/roach_demo_data?sslmode=disable
    