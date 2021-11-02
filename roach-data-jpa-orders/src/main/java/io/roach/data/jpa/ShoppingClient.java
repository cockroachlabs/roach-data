package io.roach.data.jpa;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShoppingClient implements CommandLineRunner {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ShoppingSystemFacade shoppingSystemFacade;

    @Autowired
    private DataSource dataSource;

    private String databaseVersion() {
        try {
            return new JdbcTemplate(dataSource).queryForObject("select version()", String.class);
        } catch (DataAccessException e) {
            return "unknown";
        }
    }

    @Override
    public void run(String... args) {
        logger.info("Welcome to the Shop - using {}", databaseVersion());

        logger.info("Clear all...");
        shoppingSystemFacade.clearAll();

        logger.info("Creating products...");
        shoppingSystemFacade.createProductInventory();

        logger.info("Creating customers...");
        shoppingSystemFacade.createCustomers();

        logger.info("Creating orders...");
        shoppingSystemFacade.createOrders();

        logger.info("Listing orders...");
        shoppingSystemFacade.listOrders();

        logger.info("Removing orders...");
        shoppingSystemFacade.removeOrders();
    }
}
