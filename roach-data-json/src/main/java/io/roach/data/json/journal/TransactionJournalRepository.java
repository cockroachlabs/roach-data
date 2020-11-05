package io.roach.data.json.journal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionJournalRepository extends JpaRepository<TransactionJournal, UUID> {
    @Query(value = "SELECT j FROM Journal j WHERE j.tag=:tag")
    List<TransactionJournal> findByTag(@Param("tag") String tag);

    @Query(value = "SELECT * FROM journal WHERE event_type='TRANSACTION'"
            + " AND payload ->> 'transferDate' BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    List<TransactionJournal> findBetweenTransferDates(@Param("startDate") String startDate,
                                                      @Param("endDate") String endDate);

    @Query(value =
            "WITH x AS(SELECT payload from journal where event_type='TRANSACTION' AND tag=:tag),"
                    + "items AS(SELECT json_array_elements(payload->'items') as y FROM x) "
                    + "SELECT sum((y->>'amount')::::decimal) FROM items",
            nativeQuery = true)
    BigDecimal sumTransactionLegAmounts(@Param("tag") String tag);
}
