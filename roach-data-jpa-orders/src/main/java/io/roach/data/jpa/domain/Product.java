package io.roach.data.jpa.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 128, nullable = false, unique = true)
    private String sku;

    @Column(length = 25, nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
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
