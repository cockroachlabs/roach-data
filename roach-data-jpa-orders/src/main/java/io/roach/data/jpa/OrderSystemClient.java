package io.roach.data.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.roach.data.jpa.service.OrderSystem;

@Component
public class OrderSystemClient implements CommandLineRunner {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrderSystem orderSystem;

    @Override
    public void run(String... args) {
        logger.info("Clear all...");
        orderSystem.clearAll();

        logger.info(">> Creating products...");
        orderSystem.createProductInventory();

        logger.info(">> Creating customers...");
        orderSystem.createCustomers();

        logger.info(">> Creating orders...");
        orderSystem.createOrders();

        logger.info(">> Listing orders...");
        orderSystem.listOrders();

        logger.info(">> Find by sku: {}", orderSystem.findProductBySku("CRDB-UL-ED1"));

        logger.info(">> Get total order price: {}", orderSystem.getTotalOrderPrice());

//        logger.info("Removing orders...");
//        orderSystem.removeOrders();
    }
}
