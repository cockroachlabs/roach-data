package io.roach.data.jpa.journal;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.data.jpa.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TransactionRepositoryTest extends AbstractIntegrationTest {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(1)
    public void whenCreatingTransaction_thenSuccess() {
        Account a = Account.builder()
                .withGeneratedId()
                .withName("A")
                .withBalance(BigDecimal.valueOf(50.00))
                .withAccountType("X")
                .build();
        a = accountRepository.save(a);

        Account b = Account.builder()
                .withGeneratedId()
                .withName("B")
                .withBalance(BigDecimal.valueOf(0.00))
                .withAccountType("X")
                .build();
        b = accountRepository.save(b);

        Transaction transaction = Transaction.builder()
                .withGeneratedId()
                .withBookingDate(LocalDate.now().minusDays(1))
                .withTransferDate(LocalDate.now())
                .andItem()
                .withAccount(a)
                .withAmount(BigDecimal.valueOf(-50.00))
                .withRunningBalance(BigDecimal.valueOf(-0.00))
                .withNote("debit A")
                .then()
                .andItem()
                .withAccount(b)
                .withAmount(BigDecimal.valueOf(50.00))
                .withRunningBalance(BigDecimal.valueOf(-0.00))
                .withNote("credit A")
                .then()
                .build();

        transaction = transactionRepository.save(transaction);

        assertNotNull(transaction);
    }
}
