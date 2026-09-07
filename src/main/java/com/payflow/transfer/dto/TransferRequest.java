package com.payflow.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotNull(message = "출금계좌 ID는 필수입니다.") Long fromAccountId,
        @NotNull(message = "입금계좌 ID는 필수입니다.") Long toAccountId,
        @NotNull(message = "송금금액은 필수입니다.") @Positive(message = "송금금액은 0보다 커야합니다.") Long amount){
}
