package io.roach.data.jpa.service;

import java.math.BigDecimal;

import io.roach.data.jpa.domain.Product;

public interface OrderSystem {
    void clearAll();

    void createProductInventory();

    void createCustomers();

    void createOrders();

    void listOrders();

    Product findProductBySku(String sku);

    BigDecimal getTotalOrderPrice();

    void removeOrders();
}
