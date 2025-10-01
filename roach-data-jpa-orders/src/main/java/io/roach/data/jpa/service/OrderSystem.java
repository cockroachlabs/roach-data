package io.roach.data.jpa.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.roach.data.jpa.domain.Order;
import io.roach.data.jpa.domain.Product;

public interface OrderSystem {
    void clearAll();

    void createProductInventory();

    void createCustomers();

    List<UUID> createOrders();

    List<Order> listAllOrders();

    List<Order> listAllOrderDetails();

    Order findOrderById(UUID id);

    Product findProductBySku(String sku);

    BigDecimal getTotalOrderPrice();

    void removeOrders();
}
