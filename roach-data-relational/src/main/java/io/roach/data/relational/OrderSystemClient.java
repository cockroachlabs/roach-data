package io.roach.data.relational;

import io.roach.data.relational.domain.Customer;
import io.roach.data.relational.domain.Product;
import io.roach.data.relational.service.OrderSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OrderSystemClient implements CommandLineRunner {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrderSystem orderSystem;

    @Override
    public void run(String... args) {
        logger.info("Removing everything...");
        orderSystem.clearAll();

        logger.info("Creating products...");
        orderSystem.createProductInventory();

        logger.info("Creating customers...");
        orderSystem.createCustomers();

        logger.info("Creating orders...");
        orderSystem.createOrders();

        logger.info("Listing orders...");
        orderSystem.findOrders().forEach(order -> {
            Customer c = orderSystem.findCustomerById(order.getCustomerRef().getId());
            logger.info("Order placed by {}", c.getUserName());
            logger.info("\tOrder total price: {}", order.getTotalPrice());
            logger.info("\tOrder items:");

            order.getOrderItems().forEach(orderItem -> {
                Product p = orderSystem.findProductById(orderItem.getProductRef().getId());
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


        logger.info("Find by sku: {}", orderSystem.findProductBySku("CRDB-UL-ED1"));

        logger.info("Listing customer orders...");
        orderSystem.findCustomers().forEach(customer -> {
            logger.info("Customer {} orders:", customer.getId());
            orderSystem.findOrders(customer).forEach(order -> {
                logger.info("\t{}:", order.toString());
            });
        });

        orderSystem.getTotalOrderCost((iterated, aggregated) -> {
            logger.info("Total order cost iterated: {}", iterated);
            logger.info("Total order cost aggregated: {}", aggregated);
        });
    }
}
