package io.roach.data.json.journal;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

import javax.persistence.*;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "transaction_item")
public class TransactionItem {
    public static Builder builder(Transaction.Builder parentBuilder, Consumer<TransactionItem> callback) {
        return new Builder(parentBuilder, callback);
    }

    public static class Builder {
        private final Transaction.Builder parentBuilder;

        private final Consumer<TransactionItem> callback;

        private BigDecimal amount;

        private BigDecimal runningBalance;

        private Account account;

        private String note;

        private Builder(Transaction.Builder parentBuilder, Consumer<TransactionItem> callback) {
            this.parentBuilder = parentBuilder;
            this.callback = callback;
        }

        public Builder withAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder withRunningBalance(BigDecimal runningBalance) {
            this.runningBalance = runningBalance;
            return this;
        }

        public Builder withAccount(Account account) {
            this.account = account;
            return this;
        }

        public Builder withNote(String note) {
            this.note = note;
            return this;
        }

        public Transaction.Builder then() {
            Assert.notNull(account, "account is null");

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setAccount(account);
            transactionItem.setAmount(amount);
            transactionItem.setRunningBalance(runningBalance);
            transactionItem.setNote(note);

            callback.accept(transactionItem);

            return parentBuilder;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private BigDecimal runningBalance;

    @Column(length = 128, updatable = false)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_id_ref_account"))
    @JsonIgnore
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_id_ref_transaction"))
    @JsonIgnore
    private Transaction transaction;

    protected TransactionItem() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
}
