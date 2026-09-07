package com.payflow.transfer.dto;

import com.payflow.transfer.Transfer;
import com.payflow.transfer.TransferType;

import java.time.LocalDateTime;

public record AccountTransferResponse(Long transferId, TransferType type, Long counterAccountId, Long amount, LocalDateTime createdAt) {
    public static AccountTransferResponse from (Transfer transfer, Long accountId){
        if(transfer.getFromAccountId().equals(accountId)){ // 조회한 계좌가 출금 계좌
            return new AccountTransferResponse(
                    transfer.getId(),
                    TransferType.SENT,
                    transfer.getToAccountId(),
                    transfer.getAmount(),
                    transfer.getCreatedAt()
            );
        }
        return  new AccountTransferResponse( //조회한 계좌가 입금계좌
                transfer.getId(),
                TransferType.RECEIVED,
                transfer.getFromAccountId(),
                transfer.getAmount(),
                transfer.getCreatedAt()
        );
    }

}
