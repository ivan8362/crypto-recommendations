package org.example.exception;

public class PriceFileNotFoundException extends RuntimeException {

    public PriceFileNotFoundException(String message) {
        super(message);
    }
}
