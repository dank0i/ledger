package dev.ledger;

import dev.ledger.domain.Account;
import dev.ledger.domain.TransactionLeg.Direction;
import dev.ledger.service.CsvImportService;
import dev.ledger.service.LedgerService;
import dev.ledger.service.NotFoundException;
import dev.ledger.service.UnbalancedTransactionException;
import dev.ledger.web.dto.TransactionRequest;
import dev.ledger.web.dto.TransactionRequest.LegRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LedgerServiceTest {

    @Autowired
    private LedgerService ledger;

    @Autowired
    private CsvImportService csvImport;

    private Account checking;
    private Account salary;

    @BeforeEach
    void setUp() {
        checking = ledger.createAccount("Checking", Account.Type.ASSET);
        salary = ledger.createAccount("Salary", Account.Type.INCOME);
    }

    @Test
    void rejectsTransactionWithFewerThanTwoLegs() {
        var request = new TransactionRequest("Lonely leg", null, null,
                List.of(debit(checking, "10.00")));

        assertThatThrownBy(() -> ledger.post(request))
                .isInstanceOf(UnbalancedTransactionException.class)
                .hasMessageContaining("at least 2 legs");
    }

    @Test
    void rejectsUnbalancedLegs() {
        var request = new TransactionRequest("Off by a cent", null, null,
                List.of(debit(checking, "10.00"), credit(salary, "10.01")));

        assertThatThrownBy(() -> ledger.post(request))
                .isInstanceOf(UnbalancedTransactionException.class);
    }

    @Test
    void balancedPostingUpdatesBalances() {
        ledger.post(new TransactionRequest("Paycheck", null, null,
                List.of(debit(checking, "2500.00"), credit(salary, "2500.00"))));
        ledger.post(new TransactionRequest("Bonus", null, null,
                List.of(debit(checking, "300.00"), credit(salary, "300.00"))));

        assertThat(ledger.balance(checking.getId())).isEqualByComparingTo("2800.00");
        assertThat(ledger.balance(salary.getId())).isEqualByComparingTo("-2800.00");
        // The cached balance must track the leg sum at posting time.
        assertThat(ledger.getAccount(checking.getId()).getCachedBalance())
                .isEqualByComparingTo("2800.00");
    }

    @Test
    void replayingIdempotencyKeyReturnsOriginalTransaction() {
        var request = new TransactionRequest("Paycheck", null, "pay-2026-07",
                List.of(debit(checking, "2500.00"), credit(salary, "2500.00")));

        var first = ledger.post(request);
        var replay = ledger.post(request);

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(replay.transaction().getId()).isEqualTo(first.transaction().getId());
        // No double-booking.
        assertThat(ledger.balance(checking.getId())).isEqualByComparingTo("2500.00");
    }

    @Test
    void categoryIsPersistedAndBlankIsNormalizedToNull() {
        var tagged = ledger.post(new TransactionRequest("Groceries", " Food ", null, null,
                List.of(debit(checking, "20.00"), credit(salary, "20.00"))));
        var blank = ledger.post(new TransactionRequest("Untagged", "   ", null, null,
                List.of(debit(checking, "5.00"), credit(salary, "5.00"))));

        assertThat(tagged.transaction().getCategory()).isEqualTo("Food");
        assertThat(blank.transaction().getCategory()).isNull();
    }

    @Test
    void unknownAccountIsRejected() {
        var request = new TransactionRequest("Ghost account", null, null,
                List.of(debit(checking, "5.00"),
                        new LegRequest(999999L, Direction.CREDIT, new BigDecimal("5.00"))));

        assertThatThrownBy(() -> ledger.post(request)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void statementComputesRunningBalance() {
        Instant jan1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant feb1 = Instant.parse("2026-02-01T00:00:00Z");
        Instant mar1 = Instant.parse("2026-03-01T00:00:00Z");

        ledger.post(new TransactionRequest("December pay", jan1.minusSeconds(86400), null,
                List.of(debit(checking, "100.00"), credit(salary, "100.00"))));
        ledger.post(new TransactionRequest("January pay", feb1.minusSeconds(86400), null,
                List.of(debit(checking, "40.00"), credit(salary, "40.00"))));
        ledger.post(new TransactionRequest("February pay", mar1.minusSeconds(86400), null,
                List.of(debit(checking, "60.00"), credit(salary, "60.00"))));

        var statement = ledger.statement(checking.getId(), jan1, mar1);

        assertThat(statement.openingBalance()).isEqualByComparingTo("100.00");
        assertThat(statement.lines()).hasSize(2);
        assertThat(statement.lines().get(0).runningBalance()).isEqualByComparingTo("140.00");
        assertThat(statement.lines().get(1).runningBalance()).isEqualByComparingTo("200.00");
        assertThat(statement.closingBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    void csvImportIsIdempotentPerRow() {
        String csv = """
                date,description,amount,debitAccountId,creditAccountId
                2026-07-01,July pay,2500.00,%d,%d
                2026-07-02,Referral bonus,100.00,%d,%d
                """.formatted(checking.getId(), salary.getId(), checking.getId(), salary.getId());

        var first = csvImport.importCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        var second = csvImport.importCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(first.imported()).isEqualTo(2);
        assertThat(first.duplicates()).isZero();
        assertThat(second.imported()).isZero();
        assertThat(second.duplicates()).isEqualTo(2);
        assertThat(ledger.balance(checking.getId())).isEqualByComparingTo("2600.00");
    }

    private static LegRequest debit(Account account, String amount) {
        return new LegRequest(account.getId(), Direction.DEBIT, new BigDecimal(amount));
    }

    private static LegRequest credit(Account account, String amount) {
        return new LegRequest(account.getId(), Direction.CREDIT, new BigDecimal(amount));
    }
}
