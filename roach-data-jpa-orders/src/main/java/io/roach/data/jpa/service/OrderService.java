package io.roach.data.jpa.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.data.jpa.domain.Order;
import io.roach.data.jpa.repository.OrderRepository;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public BigDecimal getTotalOrderPrice() {
        BigDecimal price = BigDecimal.ZERO;
        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            price = price.add(order.getTotalPrice());
        }
        return price;
    }
}
