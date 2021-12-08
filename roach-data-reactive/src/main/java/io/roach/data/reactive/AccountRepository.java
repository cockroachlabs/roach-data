package io.roach.data.reactive;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

@Repository
@Transactional(propagation = MANDATORY)
public interface AccountRepository extends ReactiveSortingRepository<Account, Long> {
    Flux<Account> findAllBy(Pageable pageable);

    @Query(value = "select balance from Account where id=:id")
    Mono<BigDecimal> getBalance(Long id);

    @Modifying
    @Query("update Account set balance = balance + :balance where id=:id")
    Mono<Void> updateBalance(Long id, BigDecimal balance);
}
