package ru.rsreu.kibamba.exception;

public class IncompatibleOrder extends RuntimeException {
    public IncompatibleOrder() {
    }

    public IncompatibleOrder(String message) {
        super(message);
    }

    public IncompatibleOrder(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompatibleOrder(Throwable cause) {
        super(cause);
    }
}
