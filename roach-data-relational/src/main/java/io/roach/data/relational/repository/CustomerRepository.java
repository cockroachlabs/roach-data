package io.roach.data.relational.repository;

import io.roach.data.relational.domain.Customer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends CrudRepository<Customer, UUID> {
    Optional<Customer> findByUserName(String userName);
}
