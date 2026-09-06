package com.edafa.sms_submitter.exception;

public class TargetCompanyRequiredException extends RuntimeException {
    public TargetCompanyRequiredException(String message) {
        super(message);
    }
}