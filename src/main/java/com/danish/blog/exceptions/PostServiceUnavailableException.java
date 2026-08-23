package com.danish.blog.exceptions;

public class PostServiceUnavailableException extends RuntimeException {

    public PostServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
