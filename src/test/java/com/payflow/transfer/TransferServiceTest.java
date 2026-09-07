package com.payflow.transfer;

import com.payflow.account.Account;
import com.payflow.account.AccountRepository;
import com.payflow.exception.BusinessException;
import com.payflow.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    TransferRepository transferRepository;

    @InjectMocks
    TransferService transferService;

    Account fromAccount;
    Account toAccount;

    @BeforeEach
    void setUp() {
        fromAccount = new Account("111-111", null);
        toAccount = new Account("222-222", null);

        fromAccount.deposit(10_000L);
        toAccount.deposit(5_000L);
    }

    @Test
    void 송금에_성공하면_두_계좌의_잔액이_변경되고_이력이_저장된다() {

        // given
        when(accountRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(fromAccount));

        when(accountRepository.findById(2L))
                .thenReturn(Optional.of(toAccount));

        // when
        transferService.transfer(1L, 2L, 3_000L);

        // then
        assertThat(fromAccount.getBalance()).isEqualTo(7_000L);
        assertThat(toAccount.getBalance()).isEqualTo(8_000L);

        ArgumentCaptor<Transfer> captor =
                ArgumentCaptor.forClass(Transfer.class);

        verify(transferRepository).save(captor.capture());

        Transfer savedTransfer = captor.getValue();

        assertThat(savedTransfer.getFromAccountId()).isEqualTo(1L);
        assertThat(savedTransfer.getToAccountId()).isEqualTo(2L);
        assertThat(savedTransfer.getAmount()).isEqualTo(3_000L);
    }

    @Test
    void 출금계좌가_존재하지_않으면_예외가_발생한다() {

        // given
        when(accountRepository.findByIdWithLock(1L))
                .thenReturn(Optional.empty());

        // when
        BusinessException exception = catchThrowableOfType(
                () -> transferService.transfer(1L, 2L, 3_000L),
                BusinessException.class
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void 같은_계좌로는_송금할_수_없다() {

        // given
//        when(accountRepository.findByIdWithLock(1L))
//                .thenReturn(Optional.of(fromAccount));

        // when
        BusinessException exception = catchThrowableOfType(
                () -> transferService.transfer(1L, 1L, 3_000L),
                BusinessException.class
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.SAME_ACCOUNT_TRANSFER);

        assertThat(fromAccount.getBalance()).isEqualTo(10_000L);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void 잔액이_부족하면_송금할_수_없다() {

        // given
        when(accountRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(fromAccount));

        when(accountRepository.findById(2L))
                .thenReturn(Optional.of(toAccount));

        // when
        BusinessException exception = catchThrowableOfType(
                () -> transferService.transfer(1L, 2L, 20_000L),
                BusinessException.class
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        assertThat(fromAccount.getBalance()).isEqualTo(10_000L);
        assertThat(toAccount.getBalance()).isEqualTo(5_000L);

        verify(transferRepository, never()).save(any());
    }
}