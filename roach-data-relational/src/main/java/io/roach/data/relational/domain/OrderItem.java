package io.roach.data.relational.domain;

import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

@Table("order_item")
public class OrderItem {
    public static final class Builder {
        private final Order.Builder parentBuilder;

        private final Consumer<OrderItem> callback;

        private int quantity;

        private BigDecimal unitPrice;

        private Product product;

        Builder(Order.Builder parentBuilder, Consumer<OrderItem> callback) {
            this.parentBuilder = parentBuilder;
            this.callback = callback;
        }

        public Builder withQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder withUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder withProduct(Product product) {
            this.product = product;
            return this;
        }

        public Order.Builder then() {
            if (this.unitPrice == null) {
                this.unitPrice = product.getPrice();
            }

            OrderItem orderItem = new OrderItem();
            orderItem.unitPrice = this.unitPrice;
            orderItem.quantity = this.quantity;
            orderItem.product = AggregateReference.to(this.product.getId());
            orderItem.order = AggregateReference.to(this.product.getId());

            callback.accept(orderItem);

            return parentBuilder;
        }

    }

    @Column(value = "product_id")
    private AggregateReference<Product, UUID> product;

    @Column(value = "order_id")
    private AggregateReference<Order, UUID> order;

    @Column
    private int quantity;

    @Column(value = "unit_price")
    private BigDecimal unitPrice;

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public AggregateReference<Product, UUID> getProductRef() {
        return product;
    }

    public BigDecimal totalCost() {
        if (unitPrice == null) {
            throw new IllegalStateException("unitPrice is null");
        }
        return unitPrice.multiply(new BigDecimal(quantity));
    }
}
