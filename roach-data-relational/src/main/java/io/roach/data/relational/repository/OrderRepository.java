package io.roach.data.relational.repository;

import io.roach.data.relational.domain.Order;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface OrderRepository extends CrudRepository<Order, UUID> {
    @Query(value = "select * from orders")
    Iterable<Order> findOrders();

    @Query(value = "select o.* from orders o "
            + "join customer c on o.customer_id = c.id "
            + "where c.id=:customerId")
    Iterable<Order> findOrdersByCustomerId(@Param("customerId") UUID customerId);

    @Query(value = "select sum(o.total_price) from orders o")
    BigDecimal totalOrderCost();
}
