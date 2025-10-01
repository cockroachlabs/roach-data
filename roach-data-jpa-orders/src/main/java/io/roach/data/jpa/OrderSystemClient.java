package io.roach.data.jpa;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.roach.data.jpa.domain.Customer;
import io.roach.data.jpa.domain.Order;
import io.roach.data.jpa.domain.Product;
import io.roach.data.jpa.service.OrderSystem;

@Component
public class OrderSystemClient implements CommandLineRunner {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrderSystem orderSystem;

    @Override
    public void run(String... args) {
        orderSystem.clearAll();
        orderSystem.createProductInventory();
        orderSystem.createCustomers();
        List<UUID> ids = orderSystem.createOrders();

        ids.forEach(id -> {
            Order o = orderSystem.findOrderById(id);
            print(o);
        });

        orderSystem.listAllOrders().forEach(this::print);
        orderSystem.listAllOrderDetails().forEach(this::print);

        logger.info(">> Find by sku: {}", orderSystem.findProductBySku("CRDB-UL-ED1"));
        logger.info(">> Total order price: {}", orderSystem.getTotalOrderPrice());

        orderSystem.removeOrders();
    }

    private void print(Order order) {
        Customer c = order.getCustomer();
        logger.info("""
                Order placed by: %s
                     Total cost: %s
                """.formatted(c.getUserName(), order.getTotalPrice()));
    }

    private void printDetails(Order order) {
        Customer c = order.getCustomer();

        logger.info("""
                Order placed by: %s
                     Total cost: %s
                """.formatted(c.getUserName(), order.getTotalPrice()));

        order.getOrderItems().forEach(orderItem -> {
            Product p = orderItem.getProduct();

            logger.info("""
                     Product name: %s
                    Product price: %s
                      Product sku: %s
                         Item qty: %s
                       Unit price: %s
                       Total cost: %s
                    """.formatted(
                    p.getName(),
                    p.getPrice(),
                    p.getSku(),
                    orderItem.getQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.totalCost()));
        });
    }
}
