package dev.ledger.web.dto;

import dev.ledger.domain.TransactionLeg;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionRequest(
        @NotBlank @Size(max = 255) String description,
        @Size(max = 50) String category,
        Instant occurredAt,
        @Size(max = 255) String idempotencyKey,
        @NotNull @Valid List<LegRequest> legs) {

    /** Convenience constructor for callers that do not set a category. */
    public TransactionRequest(String description, Instant occurredAt, String idempotencyKey,
                              List<LegRequest> legs) {
        this(description, null, occurredAt, idempotencyKey, legs);
    }

    public record LegRequest(
            @NotNull Long accountId,
            @NotNull TransactionLeg.Direction direction,
            @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount) {
    }
}
