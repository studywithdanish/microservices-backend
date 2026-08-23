package com.danish.blog.post.error;

public class DownstreamServiceException extends RuntimeException {

    public DownstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
