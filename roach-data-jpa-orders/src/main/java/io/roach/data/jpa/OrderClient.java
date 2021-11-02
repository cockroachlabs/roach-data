package io.roach.data.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OrderClient implements CommandLineRunner {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrderSystemFacade orderSystemFacade;

    @Override
    public void run(String... args) {
        logger.info("Clear all...");
        orderSystemFacade.clearAll();

        logger.info("Creating products...");
        orderSystemFacade.createProductInventory();

        logger.info("Creating customers...");
        orderSystemFacade.createCustomers();

        logger.info("Creating orders...");
        orderSystemFacade.createOrders();

        logger.info("Listing orders...");
        orderSystemFacade.listOrders();

        logger.info("Removing orders...");
        orderSystemFacade.removeOrders();
    }
}
