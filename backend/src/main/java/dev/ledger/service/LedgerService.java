package dev.ledger.service;

import dev.ledger.domain.Account;
import dev.ledger.domain.LedgerTransaction;
import dev.ledger.domain.TransactionLeg;
import dev.ledger.repo.AccountRepository;
import dev.ledger.repo.LedgerTransactionRepository;
import dev.ledger.repo.TransactionLegRepository;
import dev.ledger.web.dto.StatementResponse;
import dev.ledger.web.dto.TransactionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    /** Result of a posting: the transaction plus whether it was newly created. */
    public record PostResult(LedgerTransaction transaction, boolean created) {}

    private final AccountRepository accounts;
    private final LedgerTransactionRepository transactions;
    private final TransactionLegRepository legs;

    public LedgerService(AccountRepository accounts,
                         LedgerTransactionRepository transactions,
                         TransactionLegRepository legs) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.legs = legs;
    }

    @Transactional
    public Account createAccount(String name, Account.Type type) {
        return accounts.save(new Account(name, type));
    }

    @Transactional(readOnly = true)
    public List<Account> listAccounts() {
        return accounts.findAll();
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("Account " + id + " not found"));
    }

    /** Authoritative balance, always recomputed from the legs. */
    @Transactional(readOnly = true)
    public BigDecimal balance(Long accountId) {
        getAccount(accountId);
        return legs.sumBalance(accountId);
    }

    @Transactional
    public PostResult post(TransactionRequest request) {
        // Replaying a known idempotency key returns the original posting so
        // clients can safely retry after a timeout without double-booking.
        if (request.idempotencyKey() != null) {
            var existing = transactions.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                return new PostResult(existing.get(), false);
            }
        }

        validateBalanced(request.legs());

        // Lock the affected accounts before touching cached balances so two
        // concurrent postings cannot interleave read-modify-write updates.
        Set<Long> accountIds = request.legs().stream()
                .map(TransactionRequest.LegRequest::accountId)
                .collect(Collectors.toSet());
        Map<Long, Account> locked = accounts.findAllForUpdate(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        for (Long id : accountIds) {
            if (!locked.containsKey(id)) {
                throw new NotFoundException("Account " + id + " not found");
            }
        }

        Instant occurredAt = request.occurredAt() != null ? request.occurredAt() : Instant.now();
        LedgerTransaction tx = new LedgerTransaction(request.description(), occurredAt,
                request.idempotencyKey(), normalizeCategory(request.category()));
        for (TransactionRequest.LegRequest leg : request.legs()) {
            Account account = locked.get(leg.accountId());
            tx.addLeg(account, leg.direction(), leg.amount());
            account.applyToBalance(leg.direction() == TransactionLeg.Direction.DEBIT
                    ? leg.amount() : leg.amount().negate());
        }
        return new PostResult(transactions.save(tx), true);
    }

    @Transactional(readOnly = true)
    public List<LedgerTransaction> listTransactions() {
        return transactions.findAllWithLegs();
    }

    @Transactional(readOnly = true)
    public StatementResponse statement(Long accountId, Instant from, Instant to) {
        getAccount(accountId);
        BigDecimal opening = legs.sumBalanceBefore(accountId, from);
        List<TransactionLeg> statementLegs = legs.findStatementLegs(accountId, from, to);

        List<StatementResponse.Line> lines = new ArrayList<>(statementLegs.size());
        BigDecimal running = opening;
        for (TransactionLeg leg : statementLegs) {
            running = running.add(leg.signedAmount());
            lines.add(new StatementResponse.Line(
                    leg.getTransaction().getOccurredAt(),
                    leg.getTransaction().getDescription(),
                    leg.getTransaction().getCategory(),
                    leg.getDirection(), leg.getAmount(), running));
        }
        return new StatementResponse(accountId, from, to, opening, running, lines);
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.trim();
    }

    private void validateBalanced(List<TransactionRequest.LegRequest> legs) {
        if (legs == null || legs.size() < 2) {
            throw new UnbalancedTransactionException("A transaction needs at least 2 legs");
        }
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (TransactionRequest.LegRequest leg : legs) {
            if (leg.direction() == TransactionLeg.Direction.DEBIT) {
                debits = debits.add(leg.amount());
            } else {
                credits = credits.add(leg.amount());
            }
        }
        // compareTo, not equals: 10.00 and 10.0 are the same money.
        if (debits.compareTo(credits) != 0) {
            throw new UnbalancedTransactionException(
                    "Debits (" + debits + ") do not equal credits (" + credits + ")");
        }
    }
}
