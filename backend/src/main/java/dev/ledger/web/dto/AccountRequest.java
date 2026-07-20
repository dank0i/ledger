package dev.ledger.web.dto;

import dev.ledger.domain.Account;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull Account.Type type) {
}
