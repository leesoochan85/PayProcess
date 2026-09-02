package com.payflow.payflow.transfer;

import com.payflow.payflow.account.Account;
import com.payflow.payflow.account.AccountRepository;
import com.payflow.payflow.exception.BusinessException;
import com.payflow.payflow.exception.ErrorCode;
import com.payflow.payflow.transfer.dto.AccountTransferResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, Long amount){
        Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow(() ->
                new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        Account toAccount = accountRepository.findById(toAccountId).orElseThrow(() ->
                new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        if(fromAccountId.equals(toAccountId)){
            throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        fromAccount.withdraw(amount);
        toAccount.deposit(amount);

        Transfer transfer = new Transfer(fromAccountId, toAccountId, amount);
        transferRepository.save(transfer);
    }
    public List<com.payflow.payflow.transfer.TransferResponse> findAll(){
        return transferRepository.findAll().stream().map(com.payflow.payflow.transfer.TransferResponse::from).toList();
    }

    public List<com.payflow.payflow.transfer.TransferResponse> findSentTransfers(Long accountId){
        return transferRepository.findByFromAccountId(accountId).stream().map(com.payflow.payflow.transfer.TransferResponse::from).toList();
    }

    public List<com.payflow.payflow.transfer.TransferResponse>findReceivedTransfers(Long accountId){
        return transferRepository.findByToAccountId(accountId).stream().map(TransferResponse::from).toList();
    }

    public List<AccountTransferResponse> findAccountTransfers(Long accountId) {
        return transferRepository.
                findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream().map(transfer -> AccountTransferResponse.from(
                        transfer,
                        accountId)).toList();
    }
}
