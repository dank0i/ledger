package dev.ledger.repo;

import dev.ledger.domain.TransactionLeg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface TransactionLegRepository extends JpaRepository<TransactionLeg, Long> {

    @Query("""
            select coalesce(sum(case when l.direction = 'DEBIT'
                                     then l.amount else -l.amount end), 0)
            from TransactionLeg l
            where l.account.id = :accountId
            """)
    BigDecimal sumBalance(@Param("accountId") Long accountId);

    @Query("""
            select coalesce(sum(case when l.direction = 'DEBIT'
                                     then l.amount else -l.amount end), 0)
            from TransactionLeg l
            where l.account.id = :accountId and l.transaction.occurredAt < :before
            """)
    BigDecimal sumBalanceBefore(@Param("accountId") Long accountId, @Param("before") Instant before);

    @Query("""
            select l from TransactionLeg l
            join fetch l.transaction t
            where l.account.id = :accountId and t.occurredAt >= :from and t.occurredAt < :to
            order by t.occurredAt, l.id
            """)
    List<TransactionLeg> findStatementLegs(@Param("accountId") Long accountId,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to);
}
