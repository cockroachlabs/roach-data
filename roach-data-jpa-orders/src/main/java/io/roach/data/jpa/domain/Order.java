package io.roach.data.jpa.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order extends AbstractEntity<UUID> {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Customer customer;

        private final List<OrderItem> orderItems = new ArrayList<>();

        private Builder() {
        }

        public Builder withCustomer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public OrderItem.Builder andOrderItem() {
            return new OrderItem.Builder(this, orderItems::add);
        }

        public Order build() {
            if (this.customer == null) {
                throw new IllegalStateException("Missing customer");
            }
            if (this.orderItems.isEmpty()) {
                throw new IllegalStateException("Empty order");
            }
            Order order = new Order();
            order.customer = this.customer;
            order.orderItems.addAll(this.orderItems);
            order.totalPrice = order.subTotal();
            return order;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "total_price", nullable = false, updatable = false)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "order_items",
            joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "item_pos")
    @Fetch(FetchMode.SUBSELECT)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Override
    public UUID getId() {
        return id;
    }

    public Order setId(UUID id) {
        this.id = id;
        return this;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }

    public BigDecimal subTotal() {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (OrderItem oi : orderItems) {
            subTotal = subTotal.add(oi.totalCost());
        }
        return subTotal;
    }
}
