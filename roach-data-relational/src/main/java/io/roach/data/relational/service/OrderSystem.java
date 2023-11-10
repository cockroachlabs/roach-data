package io.roach.data.relational.service;

import io.roach.data.relational.domain.Customer;
import io.roach.data.relational.domain.Order;
import io.roach.data.relational.domain.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Main order system business facade.
 */
public interface OrderSystem {
    List<Product> createProductInventory();

    List<Customer> createCustomers();

    List<Order> createOrders();

    Iterable<Order> findOrders();

    Iterable<Order> findOrders(Customer customer);

    Iterable<Customer> findCustomers();

    Product findProductBySku(String sku);

    Product findProductById(UUID id);

    Customer findCustomerById(UUID id);

    void getTotalOrderCost(BiConsumer<BigDecimal,BigDecimal> result);

    void clearAll();
}
