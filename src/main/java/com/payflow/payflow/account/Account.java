package com.payflow.payflow.account;

import com.payflow.payflow.exception.ErrorCode;
import com.payflow.payflow.exception.BusinessException;
import com.payflow.payflow.user.User;
import jakarta.persistence.*;

@Entity
@Table(name="accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private Long balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    protected Account(){
    }

    public Account(String accountNumber, User user) {
        this.accountNumber = accountNumber;
        this.balance = 0L;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Long getBalance() {
        return balance;
    }

    public User getUser() {
        return user;
    }

    public void deposit(Long amount){
        if(amount <= 0){
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        this.balance+=amount;
    }

    public void withdraw(Long amount){
        if(amount<=0){
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        if(balance<amount){
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance-=amount;
    }
}
