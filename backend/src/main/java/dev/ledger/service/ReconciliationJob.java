package dev.ledger.service;

import dev.ledger.domain.Account;
import dev.ledger.repo.AccountRepository;
import dev.ledger.repo.TransactionLegRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Safety net for the cached balances: the leg sum is the source of truth, and
 * any divergence means a bug (or manual data edit) slipped past posting.
 */
@Component
public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final AccountRepository accounts;
    private final TransactionLegRepository legs;

    public ReconciliationJob(AccountRepository accounts, TransactionLegRepository legs) {
        this.accounts = accounts;
        this.legs = legs;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void reconcile() {
        int checked = 0;
        int mismatched = 0;
        for (Account account : accounts.findAll()) {
            checked++;
            BigDecimal fromLegs = legs.sumBalance(account.getId());
            if (fromLegs.compareTo(account.getCachedBalance()) != 0) {
                mismatched++;
                log.warn("Reconciliation mismatch for account {} ({}): cached={} legs={}",
                        account.getId(), account.getName(), account.getCachedBalance(), fromLegs);
            }
        }
        if (mismatched == 0) {
            log.info("Reconciliation OK: {} accounts checked", checked);
        } else {
            log.warn("Reconciliation found {} mismatched accounts out of {}", mismatched, checked);
        }
    }
}
