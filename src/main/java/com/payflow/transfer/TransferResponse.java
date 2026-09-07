package com.payflow.transfer;

import java.time.LocalDateTime;

public record TransferResponse (Long id, Long fromAccountId, Long toAccountId, Long amount, LocalDateTime createdAt) {
    public static TransferResponse from (Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount(),
                transfer.getCreatedAt()
        );
    }
}
