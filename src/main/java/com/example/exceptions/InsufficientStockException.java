package com.example.exceptions;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(int available) {
        super("Only " + available + " units are available.");
    }
}
