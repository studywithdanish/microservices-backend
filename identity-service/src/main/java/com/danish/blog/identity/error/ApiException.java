package com.danish.blog.identity.error;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
