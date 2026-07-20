package dev.ledger.web;

import dev.ledger.service.LedgerService;
import dev.ledger.web.dto.AccountRequest;
import dev.ledger.web.dto.AccountResponse;
import dev.ledger.web.dto.StatementResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    public record BalanceResponse(Long accountId, BigDecimal balance) {}

    private final LedgerService ledger;

    public AccountController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return AccountResponse.from(ledger.createAccount(request.name(), request.type()));
    }

    @GetMapping
    public List<AccountResponse> list() {
        return ledger.listAccounts().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable Long id) {
        return new BalanceResponse(id, ledger.balance(id));
    }

    @GetMapping("/{id}/statement")
    public StatementResponse statement(@PathVariable Long id,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ledger.statement(id, from, to);
    }
}
