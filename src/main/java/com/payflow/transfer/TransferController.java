package com.payflow.transfer;

import com.payflow.transfer.dto.AccountTransferResponse;
import com.payflow.transfer.dto.TransferCreateResponse;
import com.payflow.transfer.dto.TransferRequest;
import com.payflow.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public TransferCreateResponse transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        Long transferId = transferService.transfer(request.fromAccountId(), request.toAccountId(), request.amount(), idempotencyKey);
        return new TransferCreateResponse(transferId,TransferStatus.SUCCESS);
    }

    @GetMapping
    public Page<TransferResponse> getTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        return transferService.findAll(page,size);
    }

    @GetMapping("/{transferId}")
    public TransferResponse getTransfer(@PathVariable Long transferId) {
        return transferService.findById(transferId);
    }

    @GetMapping("/sent/{accountId}")
    public Page<TransferResponse>getSentTransfers(@PathVariable Long accountId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size){
        return transferService.findSentTransfers(accountId, page, size);
    }

    @GetMapping("/received/{accountId}")
    public Page<TransferResponse>getReceivedTransfers(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        return transferService.findReceivedTransfers(accountId,page,size);
    }

    @GetMapping("/accounts/{accountId}")
    public Page<AccountTransferResponse>getAccountTransfers(@PathVariable Long accountId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size){
        return transferService.findAccountTransfers(accountId,page,size);
    }
}
