package io.roach.data.jpa;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import io.roach.data.jpa.domain.Customer;
import io.roach.data.jpa.domain.Order;
import io.roach.data.jpa.domain.Product;
import io.roach.data.jpa.repository.CustomerRepository;
import io.roach.data.jpa.repository.OrderRepository;
import io.roach.data.jpa.repository.ProductRepository;

@Service
public class OrderSystemFacade {
    protected static final Logger logger = LoggerFactory.getLogger(JpaOrdersApplication.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearAll() {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "TX not active");
        orderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createProductInventory() {
        Product p1 = Product.builder()
                .withName("CockroachDB Unleashed - First Edition")
                .withPrice(new BigDecimal("199.95"))
                .withSku("CRDB-UL-ED1")
                .withQuantity(10)
                .build();

        Product p2 = Product.builder()
                .withName("CockroachDB Unleashed - Second Edition")
                .withPrice(new BigDecimal("239.95"))
                .withSku("CRDB-UL-ED2")
                .withQuantity(25)
                .build();

        productRepository.save(p1);
        productRepository.save(p2);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createCustomers() {
        Customer c1 = Customer.builder()
                .withFirstName("Winston")
                .withLastName("Atkinson")
                .withUserName("winston")
                .build();

        Customer c2 = Customer.builder()
                .withFirstName("Adolfo")
                .withLastName("Guzman")
                .withUserName("adolfo")
                .build();

        customerRepository.save(c1);
        customerRepository.save(c2);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOrders() {
        Optional<Product> p1 = productRepository.findProductBySku("CRDB-UL-ED1");
        Optional<Product> p2 = productRepository.findProductBySku("CRDB-UL-ED2");

        Optional<Customer> c1 = customerRepository.findByUserName("winston");
        Optional<Customer> c2 = customerRepository.findByUserName("adolfo");

        Assert.isTrue(p1.isPresent(), "no product 1");
        Assert.isTrue(p2.isPresent(), "no product 2");
        Assert.isTrue(c1.isPresent(), "no customer 1");
        Assert.isTrue(c2.isPresent(), "no customer 2");

        Order o1 = Order.builder()
                .withCustomer(c1.get())
                .andOrderItem()
                .withProduct(p1.get())
                .withQuantity(1)
                .then()
                .andOrderItem()
                .withProduct(p2.get())
                .withQuantity(1)
                .then()
                .build();

        orderRepository.save(o1);

        Order o2 = Order.builder()
                .withCustomer(c2.get())
                .andOrderItem()
                .withProduct(p1.get())
                .withQuantity(3)
                .then()
                .andOrderItem()
                .withProduct(p2.get())
                .withQuantity(4)
                .then()
                .build();

        orderRepository.save(o2);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void listOrders() {
        logger.info("Placed orders:");

        orderRepository.findAll().forEach(order -> {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeOrders() {
        orderRepository.findOrdersByUserName("adolfo").forEach(order -> {
            logger.info("Deleting order id {} customer {}",
                    order.getId(), order.getCustomer().getUserName());
            orderRepository.delete(order);
        });
    }
}
