# Roach Demo Data :: MyBatis

Spring Boot Demo using CockroachDB with MyBatis/JDBC.

## Run

    java -jar target/roach-demo-data.jar

With a custom db URL:

    java -jar target/roach-demo-data.jar --spring.datasource.url=jdbc:postgresql://localhost:26257/roach_demo_data?sslmode=disable
    