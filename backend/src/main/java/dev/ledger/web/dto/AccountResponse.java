package dev.ledger.web.dto;

import dev.ledger.domain.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(Long id, String name, Account.Type type, Instant createdAt, BigDecimal balance) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getType(),
                account.getCreatedAt(), account.getCachedBalance());
    }
}
