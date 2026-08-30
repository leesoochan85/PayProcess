package com.payflow.payflow.transfer;

import com.payflow.payflow.account.Account;
import com.payflow.payflow.account.AccountRepository;
import com.payflow.payflow.exception.AccountNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
    private final AccountRepository accountRepository;

    public TransferService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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

    }
}
