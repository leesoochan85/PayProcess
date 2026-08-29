package com.payflow.payflow.account;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    public AccountController(AccountService accountService){
        this.accountService=accountService;
    }

    @PostMapping
    public Account createAccount(@RequestBody CreateAccountRequest request){
        return accountService.createAccount(request.userId());
    }
    @GetMapping
    public List<Account> getAccount(){
        return accountService.getAccounts();
    }
    public record CreateAccountRequest(Long userId){

    }

    @PostMapping("/{account}/deposit")
    public Account deposit(@PathVariable Long accountId, @RequestBody DepositRequest request){
        return accountService.deposit(accountId, request.amount());
    }
    public record DepositRequest(Long amount){
    }

}
