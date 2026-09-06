package com.edafa.sms_submitter.exception;

public class DuplicateValueException  extends RuntimeException {
    public DuplicateValueException (String message) {
        super(message);
    }
}