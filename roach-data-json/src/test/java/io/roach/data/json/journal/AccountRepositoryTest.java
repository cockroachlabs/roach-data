package io.roach.data.json.journal;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.data.json.AbstractIntegrationTest;

public class AccountRepositoryTest extends AbstractIntegrationTest {
    @Autowired
    private AccountRepository accountRepository;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(3)
    public void testCreateAccount() {
        Account a = accountRepository.findByName("A");
        if (a != null) {
            accountRepository.delete(a);
        }
        accountRepository.flush();

        a = Account.builder()
                .withGeneratedId()
                .withName("A")
                .withBalance(BigDecimal.TEN)
                .withAccountType("X")
                .build();
        accountRepository.save(a);
        logger.info("Created {}", a);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(4)
    public void testRetrieveAccount() {
        Account a = accountRepository.findByName("A");
        Assertions.assertNotNull(a);
        Assertions.assertEquals(BigDecimal.TEN.setScale(2), a.getBalance());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(5)
    public void testUpdateAccount() {
        Account a = accountRepository.findByName("A");
        Assertions.assertNotNull(a);
        a.setBalance(BigDecimal.ZERO);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(6)
    public void testVerifyUpdate() {
        Account a = accountRepository.findByName("A");
        Assertions.assertEquals(BigDecimal.ZERO.setScale(2), a.getBalance());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(1)
    public void testDeleteAccount() {
        Account a = accountRepository.findByName("A");
        Assertions.assertNotNull(a);
        accountRepository.delete(a);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(2)
    public void testVerifyDeleted() {
        Account a = accountRepository.findByName("A");
        Assertions.assertNull(a);
    }
}
