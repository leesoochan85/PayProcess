package com.payflow.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계좌가 존재하지 않습니다."),

    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", "잔액이 부족합니다."),

    SAME_ACCOUNT_TRANSFER(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT_TRANSFER", "같은 계좌로는 송금할 수 없습니다."),

    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "금액은 0보다 커야 합니다."),

    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.BAD_REQUEST,"IDEMPOTENCY_KEY_CONFLICT","동일한 멱등성 키로 다른 송금 요청을 보낼 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
