package io.roach.data.json.journal;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
    Account findByName(String name);

    @Query(value = "select balance from Account where id=?1")
    BigDecimal getBalance(UUID id);

    @Modifying
    @Query("update Account set balance = balance + ?2 where id=?1")
    void updateBalance(UUID id, BigDecimal balance);
}
