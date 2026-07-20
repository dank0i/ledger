package dev.ledger.web.dto;

import dev.ledger.domain.TransactionLeg;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StatementResponse(
        Long accountId,
        Instant from,
        Instant to,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<Line> lines) {

    public record Line(Instant occurredAt, String description, String category,
                       TransactionLeg.Direction direction, BigDecimal amount,
                       BigDecimal runningBalance) {
    }
}
