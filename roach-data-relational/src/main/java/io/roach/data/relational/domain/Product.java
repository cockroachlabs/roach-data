package io.roach.data.relational.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "product")
public class Product extends AbstractEntity<UUID> {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;

        private String sku;

        private BigDecimal price;

        private int quantity;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withSku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder withPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder withQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }


        public Product build() {
            Product product = new Product();
            product.name = this.name;
            product.sku = this.sku;
            product.price = this.price;
            product.inventory = this.quantity;
            return product;
        }
    }

    @Id
    private UUID id;

    @Column
    private String name;

    @Column
    private String sku;

    @Column
    private BigDecimal price;

    @Column
    private int inventory;

    @Override
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getSku() {
        return sku;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int addInventoryQuantity(int qty) {
        this.inventory += qty;
        return inventory;
    }
}
