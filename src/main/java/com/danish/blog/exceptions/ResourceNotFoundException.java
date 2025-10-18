package com.danish.blog.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceNotFoundException extends RuntimeException {

    String resourceName;
    String fieldName;
    long fieldValue;

    public ResourceNotFoundException(String message, String resourceName, long fieldValue) {
        super(String.format("%s not found with %s : %s",message,resourceName,fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

}
