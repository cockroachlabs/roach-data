package io.roach.data.jpa.journal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.data.jpa.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionJournalTest extends AbstractIntegrationTest {
    @Autowired
    private TransactionJournalRepository repository;

    private String transactionId;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(1)
    public void whenCreatingTransactionEventInJournal_thenComputedKeyIsReturnedFromPayload() {
        Transaction transaction = Transaction.builder()
                .withGeneratedId()
                .withBookingDate(LocalDate.now().minusDays(1))
                .withTransferDate(LocalDate.now())
                .andItem()
                .withAccount(Account.builder()
                        .withGeneratedId()
                        .build())
                .withAmount(BigDecimal.valueOf(-50.00))
                .withNote("debit A")
                .then()
                .andItem()
                .withAccount(Account.builder()
                        .withGeneratedId()
                        .build())
                .withAmount(BigDecimal.valueOf(50.00))
                .withNote("credit A")
                .then()
                .build();

        TransactionJournal journal = new TransactionJournal();
        journal.setTag("cashout");
        journal.setEvent(transaction);

        journal = repository.save(journal);

        assertNotNull(journal);
        assertEquals(transaction.getId().toString(), journal.getId());

        transactionId = transaction.getId().toString();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(2)
    public void whenFindByTag_thenAtLeastOneEventIsReturned() {
        List<TransactionJournal> result = repository.findByTag("cashout");

        assertTrue(result.stream()
                .map(TransactionJournal::getId)
                .anyMatch(id -> Objects.equals(transactionId, id)));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(3)
    public void whenFindByTransferDateBetween_thenAtLeastOneEventIsReturned() {
        List<TransactionJournal> result = repository
                .findBetweenTransferDates(LocalDate.now().toString(), LocalDate.now().toString());

        assertTrue(result.stream()
                .map(TransactionJournal::getId)
                .anyMatch(id -> Objects.equals(transactionId, id)));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(4)
    public void whenComputeTransactionLegSum_thenZeroIsReturned() {
        BigDecimal result = repository
                .sumTransactionLegAmounts("cashout");

        assertEquals(BigDecimal.ZERO.setScale(1), result);
    }
}
