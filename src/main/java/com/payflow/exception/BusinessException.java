package com.payflow.exception;

public class BusinessException extends RuntimeException{
    private final ErrorCode errorcode;

    public BusinessException(ErrorCode errorcode) {
        super(errorcode.getMessage());
        this.errorcode = errorcode;
    }

    public ErrorCode getErrorCode() {
        return errorcode;
    }
}
