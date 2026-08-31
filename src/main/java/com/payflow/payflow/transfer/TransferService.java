package com.payflow.payflow.transfer;

import com.payflow.payflow.account.Account;
import com.payflow.payflow.account.AccountRepository;
import com.payflow.payflow.exception.AccountNotFoundException;
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
                new AccountNotFoundException("출금 계좌가 존재하지 않습니다."));

        Account toAccount = accountRepository.findById(toAccountId).orElseThrow(() ->
                new AccountNotFoundException("입금 계좌가 존재하지 않습니다."));

        if(fromAccountId.equals(toAccountId)){
            throw new IllegalArgumentException("같은 계좌로는 송금할 수 없습니다.");
        }

        fromAccount.withdraw(amount);
        toAccount.deposit(amount);

        Transfer transfer = new Transfer(fromAccountId, toAccountId, amount);
        transferRepository.save(transfer);
    }
    public List<TransferResponse> findAll(){
        return transferRepository.findAll().stream().map(TransferResponse::from).toList();
    }

    public List<TransferResponse> findSentTransfers(Long accountId){
        return transferRepository.findByFromAccountId(accountId).stream().map(TransferResponse::from).toList();
    }

    public List<TransferResponse>findReceivedTransfers(Long accountId){
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
