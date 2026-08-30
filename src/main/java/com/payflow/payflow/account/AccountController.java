package com.payflow.payflow.account;

import com.payflow.payflow.transfer.dto.AccountResponse;
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
    public List<AccountResponse> getAccount(){
        return accountService.getAccounts();
    }
    public record CreateAccountRequest(Long userId){

    }

    @PostMapping("/{accountId}/deposit")
    public Account deposit(@PathVariable Long accountId, @RequestBody DepositRequest request){
        return accountService.deposit(accountId, request.amount());
    }
    public record DepositRequest(Long amount){
    }

}
