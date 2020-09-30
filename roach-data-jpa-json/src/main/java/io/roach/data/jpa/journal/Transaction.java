package io.roach.data.jpa.journal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.roach.data.jpa.support.AbstractJsonDataType;
import io.roach.data.jpa.support.LocalDateDeserializer;
import io.roach.data.jpa.support.LocalDateSerializer;

@Entity
@Table(name = "transaction")
public class Transaction {
    public static class JsonType extends AbstractJsonDataType<Transaction> {
        @Override
        public Class<Transaction> returnedClass() {
            return Transaction.class;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID transactionId;

        private LocalDate bookingDate;

        private LocalDate transferDate;

        private List<TransactionItem> items = new ArrayList<>();

        public Builder withGeneratedId() {
            withId(UUID.randomUUID());
            return this;
        }

        public Builder withId(UUID id) {
            this.transactionId = id;
            return this;
        }

        public Builder withBookingDate(LocalDate bookingDate) {
            this.bookingDate = bookingDate;
            return this;
        }

        public Builder withTransferDate(LocalDate transferDate) {
            this.transferDate = transferDate;
            return this;
        }

        public TransactionItem.Builder andItem() {
            return TransactionItem.builder(this, item -> items.add(item));
        }

        public Transaction build() {
            return new Transaction(transactionId, bookingDate, transferDate, items);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Basic
    @Column(name = "transfer_date", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate transferDate;

    @Basic
    @Column(name = "booking_date", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate bookingDate;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "transaction")
    private List<TransactionItem> items;

    public Transaction() {
    }

    protected Transaction(UUID id,
                          LocalDate bookingDate,
                          LocalDate transferDate,
                          List<TransactionItem> items) {
        this.id = id;
        this.bookingDate = bookingDate;
        this.transferDate = transferDate;
        this.items = items;

        items.forEach(transactionItem -> transactionItem.setTransaction(this));
    }

    @PrePersist
    protected void onCreate() {
        if (bookingDate == null) {
            bookingDate = LocalDate.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public List<TransactionItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
