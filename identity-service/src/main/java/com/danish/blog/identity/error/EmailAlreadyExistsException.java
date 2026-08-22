package com.danish.blog.identity.error;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("A user with email " + email + " already exists");
    }
}
