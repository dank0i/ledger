package dev.ledger.service;

/** Thrown when a posting violates the double-entry invariant. Maps to 422. */
public class UnbalancedTransactionException extends RuntimeException {

    public UnbalancedTransactionException(String message) {
        super(message);
    }
}
