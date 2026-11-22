package com.acxiom.crm.exception;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("Access denied: insufficient privileges to perform this operation");
    }
}
