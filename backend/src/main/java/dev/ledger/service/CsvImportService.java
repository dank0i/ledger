package dev.ledger.service;

import dev.ledger.domain.TransactionLeg;
import dev.ledger.web.dto.TransactionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

/**
 * Imports external transactions from CSV rows of the form:
 * date,description,amount,debitAccountId,creditAccountId
 */
@Service
public class CsvImportService {

    public record ImportResult(int imported, int duplicates) {}

    private final LedgerService ledger;

    public CsvImportService(LedgerService ledger) {
        this.ledger = ledger;
    }

    // One transaction for the whole file: a bad row rolls back the entire
    // import, so a re-upload after fixing it cannot half-duplicate the file.
    @Transactional
    public ImportResult importCsv(InputStream in) {
        int imported = 0;
        int duplicates = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || (lineNumber == 1 && line.toLowerCase().startsWith("date,"))) {
                    continue;
                }
                if (importRow(line, lineNumber)) {
                    imported++;
                } else {
                    duplicates++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ImportResult(imported, duplicates);
    }

    private boolean importRow(String line, int lineNumber) {
        String[] fields = line.split(",", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("Line " + lineNumber + ": expected 5 fields, got " + fields.length);
        }
        Instant occurredAt;
        BigDecimal amount;
        long debitAccountId;
        long creditAccountId;
        try {
            occurredAt = LocalDate.parse(fields[0].trim()).atStartOfDay(ZoneOffset.UTC).toInstant();
            amount = new BigDecimal(fields[2].trim());
            debitAccountId = Long.parseLong(fields[3].trim());
            creditAccountId = Long.parseLong(fields[4].trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Line " + lineNumber + ": " + e.getMessage());
        }

        // Hashing the raw row makes re-uploads of the same file (or overlapping
        // exports) naturally idempotent without the source assigning ids.
        String key = "csv-" + sha256(line);
        var request = new TransactionRequest(fields[1].trim(), occurredAt, key, List.of(
                new TransactionRequest.LegRequest(debitAccountId, TransactionLeg.Direction.DEBIT, amount),
                new TransactionRequest.LegRequest(creditAccountId, TransactionLeg.Direction.CREDIT, amount)));
        return ledger.post(request).created();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
