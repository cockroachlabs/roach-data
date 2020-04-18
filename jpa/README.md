# Roach Demo Data :: JPA/Hibernate

Spring Boot Demo using CockroachDB with JPA/Hibernate.
    
## Run

    java -jar target/roach-demo-data.jar

With a custom DB URL:

    java -jar target/roach-demo-data.jar --spring.datasource.url=jdbc:postgresql://localhost:26257/roach_demo_data?sslmode=disable
    