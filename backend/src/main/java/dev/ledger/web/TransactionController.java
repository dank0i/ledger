package dev.ledger.web;

import dev.ledger.service.LedgerService;
import dev.ledger.web.dto.TransactionRequest;
import dev.ledger.web.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final LedgerService ledger;

    public TransactionController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> post(@Valid @RequestBody TransactionRequest request) {
        var result = ledger.post(request);
        // 201 for a new posting, 200 when an idempotency key was replayed and
        // the original transaction is returned instead.
        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(TransactionResponse.from(result.transaction()));
    }

    @GetMapping
    public List<TransactionResponse> list() {
        return ledger.listTransactions().stream().map(TransactionResponse::from).toList();
    }
}
