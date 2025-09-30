package io.roach.data.jpa.service;

import java.math.BigDecimal;
import java.util.List;

import io.roach.data.jpa.domain.Order;
import io.roach.data.jpa.domain.Product;

public interface OrderSystem {
    void clearAll();

    void createProductInventory();

    void createCustomers();

    void createOrders();

    List<Order> listAllOrders();

    List<Order> listAllOrderDetails();

    Product findProductBySku(String sku);

    BigDecimal getTotalOrderPrice();

    void removeOrders();
}
