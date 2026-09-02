package com.payflow.payflow.transfer;

import com.payflow.payflow.account.Account;
import com.payflow.payflow.account.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class TransferTransactionTest {

    @Autowired
    TransferService transferService;

    @Autowired
    AccountRepository accountRepository;

    @MockitoBean
    TransferRepository transferRepository;

    Account fromAccount;
    Account toAccount;

    @BeforeEach
    void setUp() {

        accountRepository.deleteAll();

        fromAccount = new Account("111-111", null);
        toAccount = new Account("222-222", null);

        fromAccount.deposit(10_000L);
        toAccount.deposit(5_000L);

        fromAccount = accountRepository.save(fromAccount);
        toAccount = accountRepository.save(toAccount);
    }

    @Test
    void 송금이력_저장에_실패하면_계좌잔액도_롤백된다() {

        // given
        Long fromAccountId = fromAccount.getId();
        Long toAccountId = toAccount.getId();

        when(transferRepository.save(any(Transfer.class)))
                .thenThrow(new RuntimeException("송금 이력 저장 실패"));

        // when & then
        assertThatThrownBy(() ->
                transferService.transfer(
                        fromAccountId,
                        toAccountId,
                        3_000L
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("송금 이력 저장 실패");

        // DB에서 다시 조회
        Account reloadedFromAccount =
                accountRepository.findById(fromAccountId)
                        .orElseThrow();

        Account reloadedToAccount =
                accountRepository.findById(toAccountId)
                        .orElseThrow();

        // rollback 되었으므로 원래 잔액이어야 한다.
        assertThat(reloadedFromAccount.getBalance())
                .isEqualTo(10_000L);

        assertThat(reloadedToAccount.getBalance())
                .isEqualTo(5_000L);
    }
}