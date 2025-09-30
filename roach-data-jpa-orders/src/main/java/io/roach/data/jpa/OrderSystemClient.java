package io.roach.data.jpa;

import java.util.List;

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
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrderSystem orderSystem;

    @Override
    public void run(String... args) {
        orderSystem.clearAll();
        orderSystem.createProductInventory();
        orderSystem.createCustomers();
        orderSystem.createOrders();

        List<Order> orderList = orderSystem.listAllOrders();
        list(orderList);

        List<Order> orderList2 = orderSystem.listAllOrderDetails();
        listAllDetails(orderList2);

        logger.info(">> Find by sku: {}", orderSystem.findProductBySku("CRDB-UL-ED1"));
        logger.info(">> Get total order price: {}", orderSystem.getTotalOrderPrice());

        logger.info("Removing orders...");
        orderSystem.removeOrders();
    }

    private void list(List<Order> orderList) {
        orderList.forEach(order -> {
            Customer c = order.getCustomer();
            logger.info(">> Order placed by {}", c.getUserName());
            logger.info("\tOrder total price: {}", order.getTotalPrice());
            logger.info("\tOrder items:");
        });
    }

    private void listAllDetails(List<Order> orderList) {
        orderList.forEach(order -> {
            Customer c = order.getCustomer();
            logger.info(">> Order placed by {}", c.getUserName());
            logger.info("\tOrder total price: {}", order.getTotalPrice());
            logger.info("\tOrder items:");

            order.getOrderItems().forEach(orderItem -> {
                Product p = orderItem.getProduct();
                logger.info("\t\t{} price: {} sku: {} qty: {} unit price: {} cost: {}",
                        p.getName(),
                        p.getPrice(),
                        p.getSku(),
                        orderItem.getQuantity(),
                        orderItem.getUnitPrice(),
                        orderItem.totalCost()
                );
            });
        });
    }
}
