package dev.ledger.service;

/** Thrown when a referenced resource does not exist. Maps to 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
