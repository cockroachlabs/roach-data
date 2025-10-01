package io.roach.data.jpa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;

import io.roach.data.jpa.domain.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Modifying
    @Query(value = "delete from order_items where 1=1", nativeQuery = true)
    void deleteAllOrderItems();

    @Query(value = "from Order o "
                   + "join fetch o.customer c where o.id=:id")
    Optional<Order> findOrderById(@Param("id") UUID id);

    @Query(value = "from Order o "
                   + "join fetch o.customer c")
    List<Order> findAllOrders();

    @Query(value = "from Order o "
                   + "join fetch o.customer "
                   + "join fetch o.orderItems oi "
                   + "join fetch oi.product")
    @QueryHints({
            @QueryHint(name = "org.hibernate.comment", value = "replaceJoinWithInnerJoin,appendForShare")
    })
    List<Order> findAllOrderDetails();

    @Query(value = "from Order o "
                   + "join fetch o.customer c "
                   + "where c.userName=:userName")
    List<Order> findOrdersByUserName(@Param("userName") String userName);
}
