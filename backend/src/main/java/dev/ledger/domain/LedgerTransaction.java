package dev.ledger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ledger_transaction")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(length = 50)
    private String category;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<TransactionLeg> legs = new ArrayList<>();

    protected LedgerTransaction() {
        // for JPA
    }

    public LedgerTransaction(String description, Instant occurredAt, String idempotencyKey, String category) {
        this.description = description;
        this.occurredAt = occurredAt;
        this.idempotencyKey = idempotencyKey;
        this.category = category;
    }

    public void addLeg(Account account, TransactionLeg.Direction direction, java.math.BigDecimal amount) {
        legs.add(new TransactionLeg(this, account, direction, amount));
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCategory() {
        return category;
    }

    public List<TransactionLeg> getLegs() {
        return legs;
    }
}
