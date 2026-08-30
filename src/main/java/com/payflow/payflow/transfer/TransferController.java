package com.payflow.payflow.transfer;

import com.payflow.payflow.transfer.dto.TransferRequest;
import com.payflow.payflow.transfer.dto.TransferResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public String transfer(@RequestBody TransferRequest request) {
        transferService.transfer(request.fromAccountId(), request.toAccountId(), request.amount());
        return String.valueOf(new TransferResponse("송금 성공", request.amount()));
    }
}
