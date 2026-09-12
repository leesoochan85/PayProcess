package com.payflow.transfer.dto;

import com.payflow.transfer.Transfer;
import com.payflow.transfer.TransferStatus;

import java.time.LocalDateTime;

public record TransferResponse (Long id, Long fromAccountId, Long toAccountId, Long amount,
                                TransferStatus status, LocalDateTime createdAt) {
    public static TransferResponse from (Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt()
        );
    }
}
