package io.roach.data.relational.repository;

import io.roach.data.relational.domain.Product;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends CrudRepository<Product, UUID> {
    @Lock(LockMode.PESSIMISTIC_READ)
    Optional<Product> findProductBySku(String sku);

    @Query("select p from product p where p.sku=:sku")
    Optional<Product> findProductBySkuNoLock(@Param("sku") String sku);
}
