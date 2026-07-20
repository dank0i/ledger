package dev.ledger.repo;

import dev.ledger.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // Rows are locked in id order so two concurrent postings touching the
    // same accounts always acquire locks in the same sequence (no deadlock).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> findAllForUpdate(@Param("ids") Collection<Long> ids);
}
