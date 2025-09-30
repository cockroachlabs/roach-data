package io.roach.data.jpa.domain;

import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
@NamedQueries({
        @NamedQuery(
                name = "Customer.findByUserName",
                query = "from Customer u where u.userName = :userName"
        )
})
public class Customer extends AbstractEntity<UUID> {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String userName;

        private String firstName;

        private String lastName;

        private Builder() {
        }

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Customer build() {
            Customer customer = new Customer();
            customer.userName = this.userName;
            customer.firstName = this.firstName;
            customer.lastName = this.lastName;
            return customer;
        }
    }

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID id;

    @Column(name = "user_name", length = 15, nullable = false, unique = true)
    private String userName;

    @Column(name = "first_name", length = 45)
    private String firstName;

    @Column(name = "last_name", length = 45)
    private String lastName;

    @Override
    public UUID getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
