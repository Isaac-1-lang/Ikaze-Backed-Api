package com.ecommerce.Exception;

/**
 * Custom exception for return-related operations
 */
public class ReturnException extends RuntimeException {
    
    private final String errorCode;
    
    public ReturnException(String message) {
        super(message);
        this.errorCode = "RETURN_ERROR";
    }
    
    public ReturnException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ReturnException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "RETURN_ERROR";
    }
    
    public ReturnException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    // Specific return exception types
    public static class ReturnPeriodExpiredException extends ReturnException {
        public ReturnPeriodExpiredException(String message) {
            super("RETURN_PERIOD_EXPIRED", message);
        }
    }
    
    public static class ReturnAlreadyExistsException extends ReturnException {
        public ReturnAlreadyExistsException(String message) {
            super("RETURN_ALREADY_EXISTS", message);
        }
    }
    
    public static class ReturnNotEligibleException extends ReturnException {
        public ReturnNotEligibleException(String message) {
            super("RETURN_NOT_ELIGIBLE", message);
        }
    }
    
    public static class InvalidReturnStatusException extends ReturnException {
        public InvalidReturnStatusException(String message) {
            super("INVALID_RETURN_STATUS", message);
        }
    }
    
    public static class ReturnNotFoundException extends ReturnException {
        public ReturnNotFoundException(String message) {
            super("RETURN_NOT_FOUND", message);
        }
    }
    
    public static class AppealAlreadyExistsException extends ReturnException {
        public AppealAlreadyExistsException(String message) {
            super("APPEAL_ALREADY_EXISTS", message);
        }
    }
    
    public static class AppealNotAllowedException extends ReturnException {
        public AppealNotAllowedException(String message) {
            super("APPEAL_NOT_ALLOWED", message);
        }
    }
    
    public static class InvalidWarehouseAssignmentException extends ReturnException {
        public InvalidWarehouseAssignmentException(String message) {
            super("INVALID_WAREHOUSE_ASSIGNMENT", message);
        }
    }
    
    public static class QualityControlFailedException extends ReturnException {
        public QualityControlFailedException(String message) {
            super("QUALITY_CONTROL_FAILED", message);
        }
    }
    
    public static class FraudSuspicionException extends ReturnException {
        public FraudSuspicionException(String message) {
            super("FRAUD_SUSPICION", message);
        }
    }
}
