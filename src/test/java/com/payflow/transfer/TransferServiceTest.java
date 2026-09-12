package com.payflow.transfer;

import com.payflow.account.Account;
import com.payflow.account.AccountRepository;
import com.payflow.exception.BusinessException;
import com.payflow.exception.ErrorCode;
import com.payflow.idempotency.IdempotencyKeyRepository;
import com.payflow.transfer.dto.AccountTransferResponse;
import com.payflow.transfer.dto.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    TransferRepository transferRepository;

    @Mock
    IdempotencyKeyRepository idempotencyKeyRepository;

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

    @Test
    void 송금이_성공하면_거래상태가_SUCCESS로_저장된다(){
        Long amount = 3000L;

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        ArgumentCaptor<Transfer>captor=ArgumentCaptor.forClass(Transfer.class);

        //when
        transferService.transfer(1L,2L,3_000L);

        //then
        verify(transferRepository).save(captor.capture());
        Transfer savedTransfer = captor.getValue();
        assertThat(savedTransfer.getStatus()).isEqualTo(TransferStatus.SUCCESS);
    }

    @Test
    void 거래_ID로_거래를_조회한다(){
        Transfer transfer = new Transfer(1L,2L,3000L);
        transfer.success();
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        TransferResponse response = transferService.findById(1L);

        assertThat(response.fromAccountId()).isEqualTo(1L);
        assertThat(response.toAccountId()).isEqualTo(2L);
        assertThat(response.amount()).isEqualTo(3000L);
        assertThat(response.status()).isEqualTo(TransferStatus.SUCCESS);
    }
    @Test
    void 존재하지_않는_거래를_조회하면_예외가_발생한다(){
        when(transferRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, ()-> transferService.findById(999L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TRANSFER_NOT_FOUND);
    }

    @Test
    void 거래내역을_페이지단위로_조회한다() {
        // given
        Transfer transfer1 = new Transfer(1L, 2L, 3000L);
        Transfer transfer2 = new Transfer(1L, 3L, 2000L);

        transfer1.success();
        transfer2.success();

        List<Transfer> transfers = List.of(transfer1, transfer2);

        Page<Transfer> page = new PageImpl<>(transfers);

        when(transferRepository.findAll(any(Pageable.class))).thenReturn(page);

        // when
        Page<TransferResponse> result = transferService.findAll(0, 20);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).amount()).isEqualTo(3000L);
        assertThat(result.getContent().get(1).amount()).isEqualTo(2000L);
    }

    @Test
    void 보낸_거래내역을_페이지단위로_조회한다() {
        Transfer transfer = new Transfer(1L, 2L, 3000L);

        transfer.success();

        Page<Transfer> page = new PageImpl<>(List.of(transfer));

        when(transferRepository.findByFromAccountId(eq(1L), any(Pageable.class) )).thenReturn(page);

        Page<TransferResponse> result = transferService.findSentTransfers( 1L, 0, 20 );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).fromAccountId()).isEqualTo(1L);
    }

    @Test
    void 받은_거래내역을_페이지단위로_조회한다() {
        Transfer transfer = new Transfer(1L, 2L, 3000L);

        transfer.success();

        Page<Transfer> page = new PageImpl<>(List.of(transfer));

        when(transferRepository.findByToAccountId( eq(2L), any(Pageable.class) )).thenReturn(page);

        Page<TransferResponse> result = transferService.findReceivedTransfers( 2L, 0, 20 );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).toAccountId()).isEqualTo(2L);
    }

    @Test
    void 계좌기준_거래내역을_페이지단위로_조회한다() {
        Transfer transfer = new Transfer(1L, 2L, 3000L);

        transfer.success();

        Page<Transfer> page = new PageImpl<>(List.of(transfer));

        when(
                transferRepository
                        .findByFromAccountIdOrToAccountId( eq(1L), eq(1L), any(Pageable.class) )
        ).thenReturn(page);

        Page<AccountTransferResponse> result =
                transferService.findAccountTransfers( 1L, 0, 20 );

        assertThat(result.getContent()).hasSize(1);

        AccountTransferResponse response = result.getContent().get(0);

        assertThat(response.type()) .isEqualTo(TransferType.SENT);

        assertThat(response.counterAccountId()) .isEqualTo(2L);

        assertThat(response.amount()) .isEqualTo(3000L);
    }
}