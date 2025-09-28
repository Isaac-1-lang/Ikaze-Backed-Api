package com.ecommerce.Exception;

public class CheckoutValidationException extends RuntimeException {
    
    private final String errorCode;
    private final String userMessage;

    public CheckoutValidationException(String errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public CheckoutValidationException(String errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
