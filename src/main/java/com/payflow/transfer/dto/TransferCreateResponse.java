package com.payflow.transfer.dto;

import com.payflow.transfer.TransferStatus;

public record TransferCreateResponse(Long transferId, TransferStatus status) {
}
