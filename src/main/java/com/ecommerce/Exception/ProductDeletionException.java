package com.ecommerce.exception;

/**
 * Exception thrown when a product cannot be deleted due to business rules
 */
public class ProductDeletionException extends RuntimeException {

    public ProductDeletionException(String message) {
        super(message);
    }

    public ProductDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
