package dev.ledger.repo;

import dev.ledger.domain.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {

    // Fetch joins because open-in-view is off: legs and accounts must be
    // loaded before the transaction (the database one) ends.
    @Query("""
            select t from LedgerTransaction t
            join fetch t.legs l join fetch l.account
            where t.idempotencyKey = :key
            """)
    Optional<LedgerTransaction> findByIdempotencyKey(@Param("key") String key);

    @Query("""
            select t from LedgerTransaction t
            join fetch t.legs l join fetch l.account
            order by t.occurredAt desc, t.id desc
            """)
    List<LedgerTransaction> findAllWithLegs();
}
