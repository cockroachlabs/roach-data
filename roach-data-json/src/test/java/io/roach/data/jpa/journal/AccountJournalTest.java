package io.roach.data.json.journal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Repeat;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.data.json.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

public class AccountJournalTest extends AbstractIntegrationTest {
    @Autowired
    private AccountJournalRepository repository;

    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    @Commit
    @Order(1)
    public void whenCreatingAccountEventInJournal_thenComputedKeyIsReturnedFromPayload() {
        Account account1 = Account.builder()
                .withGeneratedId()
                .withAccountType("asset")
                .withName("abc")
                .withBalance(BigDecimal.valueOf(250.00))
                .build();
        Account account2 = Account.builder()
                .withGeneratedId()
                .withAccountType("expense")
                .withName("def")
                .withBalance(BigDecimal.valueOf(500.00))
                .build();

        AccountJournal journal1 = new AccountJournal();
        journal1.setTag("asset");
        journal1.setEvent(account1);
        journal1 = repository.save(journal1);

        assertNotNull(journal1);
        assertEquals(account1.getId().toString(), journal1.getId());

        AccountJournal journal2 = new AccountJournal();
        journal2.setTag("expense");
        journal2.setEvent(account2);
        journal2 = repository.save(journal2);

        assertNotNull(journal2);
        assertEquals(account2.getId().toString(), journal2.getId());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(2)
    public void whenFindBalanceBetween_thenAtLeastOneJournalIsReturned() {
        List<AccountJournal> result = repository.findWithBalanceBetween(
                BigDecimal.valueOf(250.00), BigDecimal.valueOf(500.00));
        assertTrue(result.size() > 0);
    }
}
