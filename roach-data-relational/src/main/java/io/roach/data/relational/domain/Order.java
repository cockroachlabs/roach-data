package io.roach.data.relational.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.*;

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
            order.customer = AggregateReference.to(this.customer.getId());
            order.orderItems.addAll(this.orderItems);
            order.totalPrice = order.subTotal();
            return order;
        }
    }

    @Id
    private UUID id;

    @Column(value = "total_price")
    private BigDecimal totalPrice;

    @Column(value = "customer_id")
    private AggregateReference<Customer, UUID> customer;

    @MappedCollection(idColumn = "order_id", keyColumn = "order_id")
    private Set<OrderItem> orderItems = new HashSet<>();

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

    public AggregateReference<Customer, UUID> getCustomerRef() {
        return customer;
    }

    public Collection<OrderItem> getOrderItems() {
        return Collections.unmodifiableSet(orderItems);
    }

    public BigDecimal subTotal() {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (OrderItem oi : orderItems) {
            subTotal = subTotal.add(oi.totalCost());
        }
        return subTotal;
    }
}
