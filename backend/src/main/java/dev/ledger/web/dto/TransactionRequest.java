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
        Instant occurredAt,
        @Size(max = 255) String idempotencyKey,
        @NotNull @Valid List<LegRequest> legs) {

    public record LegRequest(
            @NotNull Long accountId,
            @NotNull TransactionLeg.Direction direction,
            @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount) {
    }
}
