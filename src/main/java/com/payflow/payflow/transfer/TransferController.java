package com.payflow.payflow.transfer;

import com.payflow.payflow.transfer.dto.AccountTransferResponse;
import com.payflow.payflow.transfer.dto.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public String transfer(@Valid @RequestBody TransferRequest request) {
        transferService.transfer(request.fromAccountId(), request.toAccountId(), request.amount());
        return "송금 성공";
    }

    @GetMapping
    public List<com.payflow.payflow.transfer.TransferResponse> getTransfers(){
        return transferService.findAll();
    }
    @GetMapping("/sent/{accountId}")
    public List<com.payflow.payflow.transfer.TransferResponse>getTransfers(@PathVariable Long accountId){
        return transferService.findSentTransfers(accountId);
    }
    @GetMapping("/received/{accountId}")
    public List<com.payflow.payflow.transfer.TransferResponse>getReceivedTransfers(@PathVariable Long accountId){
        return transferService.findReceivedTransfers(accountId);
    }
    @GetMapping("/accounts/{accountId}")
    public List<AccountTransferResponse>getAll(@PathVariable Long accountId){
        return transferService.findAccountTransfers(accountId);
    }

}
