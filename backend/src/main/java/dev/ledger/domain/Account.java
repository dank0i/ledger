package dev.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account")
public class Account {

    public enum Type { ASSET, LIABILITY, INCOME, EXPENSE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Denormalized sum of legs (debits minus credits), kept in sync at posting
    // time. The reconciliation job cross-checks it against the leg sum.
    @Column(name = "cached_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal cachedBalance;

    protected Account() {
        // for JPA
    }

    public Account(String name, Type type) {
        this.name = name;
        this.type = type;
        this.createdAt = Instant.now();
        this.cachedBalance = BigDecimal.ZERO;
    }

    public void applyToBalance(BigDecimal signedAmount) {
        this.cachedBalance = this.cachedBalance.add(signedAmount);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getCachedBalance() {
        return cachedBalance;
    }
}
