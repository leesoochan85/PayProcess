package com.payflow.payflow.account;

import com.payflow.payflow.user.User;
import com.payflow.payflow.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public Account createAccount(Long userId){
        User user = userRepository.findById(userId).orElseThrow(()
                ->new IllegalArgumentException("사용자가 존재하지 않습니다.")
        );

        String accountNumber = UUID.randomUUID().toString().substring(0,8);
        Account account = new Account(accountNumber, user);

        return accountRepository.save(account);
    }
    public List<Account> getAccounts(){
        return accountRepository.findAll();
    }

    @Transactional //dirty checking
    public Account deposit(Long accountId ,Long amount){
        Account account = accountRepository.findById(accountId).orElseThrow(()
                ->new IllegalArgumentException("계좌가 존재하지 않습니다.")
        );
        account.deposit(amount);
        return  account;
    }
}
