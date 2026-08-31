package com.payflow.payflow.transfer.dto;

public record TransferRequest(Long fromAccountId, Long toAccountId, Long amount){
}
