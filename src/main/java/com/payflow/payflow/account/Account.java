package com.payflow.payflow.account;

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
            throw new IllegalArgumentException("입금 금액은 0보다 커야합니다.");
        }
        this.balance+=amount;
    }

    public void withdraw(Long amount){
        if(amount<=0){
            throw new IllegalArgumentException("출금금액은 0보다 커야합니다.");
        }
        if(balance<amount){
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.balance-=amount;
    }
}
