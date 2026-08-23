package com.danish.blog.post.error;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
