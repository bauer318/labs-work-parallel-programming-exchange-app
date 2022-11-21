package ru.rsreu.kibamba.exception;

public class InsufficientBalance extends RuntimeException {
    public InsufficientBalance() {
    }

    public InsufficientBalance(String message) {
        super(message);
    }

    public InsufficientBalance(String message, Throwable cause) {
        super(message, cause);
    }

    public InsufficientBalance(Throwable cause) {
        super(cause);
    }
}
