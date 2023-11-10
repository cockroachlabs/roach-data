package io.roach.data.relational.service;

import io.roach.data.relational.domain.Order;
import io.roach.data.relational.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public BigDecimal getTotalOrderCost() {
        BigDecimal price = BigDecimal.ZERO;
        // N+1 query
        Iterable<Order> orders = orderRepository.findOrders();
        for (Order order : orders) {
            price = price.add(order.getTotalPrice());
        }
        return price;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public BigDecimal getTotalOrderCostAggregate() {
        return orderRepository.totalOrderCost();
    }
}
