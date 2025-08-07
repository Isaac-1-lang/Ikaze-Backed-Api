package com.ecommerce.Exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomException extends RuntimeException {

    private String message;

    public CustomException(String message) {
        super(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
