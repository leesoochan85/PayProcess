package com.payflow.payflow.transfer.dto;

public record AccountResponse
        (Long id, String accountNumber, Long balance, Long userId, String userName) {

}

