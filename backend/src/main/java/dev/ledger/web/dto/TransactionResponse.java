package dev.ledger.web.dto;

import dev.ledger.domain.LedgerTransaction;
import dev.ledger.domain.TransactionLeg;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionResponse(
        Long id,
        String description,
        String category,
        Instant occurredAt,
        String idempotencyKey,
        List<LegResponse> legs) {

    public record LegResponse(Long id, Long accountId, String accountName,
                              TransactionLeg.Direction direction, BigDecimal amount) {
    }

    public static TransactionResponse from(LedgerTransaction tx) {
        List<LegResponse> legs = tx.getLegs().stream()
                .map(l -> new LegResponse(l.getId(), l.getAccount().getId(), l.getAccount().getName(),
                        l.getDirection(), l.getAmount()))
                .toList();
        return new TransactionResponse(tx.getId(), tx.getDescription(), tx.getCategory(),
                tx.getOccurredAt(), tx.getIdempotencyKey(), legs);
    }
}
