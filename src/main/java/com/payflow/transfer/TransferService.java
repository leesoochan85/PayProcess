package com.payflow.transfer;

import com.payflow.account.Account;
import com.payflow.account.AccountRepository;
import com.payflow.exception.BusinessException;
import com.payflow.exception.ErrorCode;
import com.payflow.idempotency.IdempotencyKey;
import com.payflow.idempotency.IdempotencyKeyRepository;
import com.payflow.transfer.dto.AccountTransferResponse;
import com.payflow.transfer.dto.TransferResponse;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public TransferService(AccountRepository accountRepository,
                           TransferRepository transferRepository,
                           IdempotencyKeyRepository idempotencyKeyRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional
    public Long transfer(Long fromAccountId, Long toAccountId, Long amount, String idempotencyKey) {
        if(fromAccountId.equals(toAccountId)){
            throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        Account fromAccount = accountRepository.findByIdWithLock(fromAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        IdempotencyKey existingKey = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey).orElse(null);

        if(existingKey!=null){
            if(!existingKey.isSameRequest(fromAccountId,toAccountId,amount)){
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
            return existingKey.getTransferId();
        }

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() ->new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        fromAccount.withdraw(amount);
        toAccount.deposit(amount);

        Transfer transfer = new Transfer(fromAccountId, toAccountId, amount);
        transfer.success();
        Transfer savedTransfer = transferRepository.save(transfer);
        IdempotencyKey savedKey = new IdempotencyKey(idempotencyKey, savedTransfer.getId(),fromAccountId,toAccountId,amount);
        idempotencyKeyRepository.save(savedKey);

        return savedTransfer.getId();
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, Long amount){

        if(fromAccountId.equals(toAccountId)){
            throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        Account fromAccount = accountRepository.findByIdWithLock(fromAccountId).orElseThrow(() ->
                new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        Account toAccount = accountRepository.findById(toAccountId).orElseThrow(() ->
                new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));


        fromAccount.withdraw(amount);
        toAccount.deposit(amount);

        Transfer transfer = new Transfer(fromAccountId, toAccountId, amount);
        transfer.success();
        transferRepository.save(transfer);
    }

    public Page<TransferResponse> findAll(int page, int size){
        return transferRepository.findAll(createPageable(page,size)).map(TransferResponse::from);
    }

    public Page<TransferResponse> findSentTransfers(Long accountId, int page, int size){
        return transferRepository.findByFromAccountId(accountId,createPageable(page,size)).map(TransferResponse::from);
    }

    public Page<TransferResponse> findReceivedTransfers(Long accountId, int page, int size){
        return transferRepository.findByToAccountId(accountId,createPageable(page,size)).map(TransferResponse::from);
    }

    public Page<AccountTransferResponse> findAccountTransfers(Long accountId, int page, int size) {
        return transferRepository.findByFromAccountIdOrToAccountId(accountId, accountId,
                createPageable(page,size)).map(transfer->AccountTransferResponse.from(transfer,accountId));
    }

    public TransferResponse findById(Long transferId){
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
        return TransferResponse.from(transfer);
    }

    private Pageable createPageable(int page, int size){
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt"));
    }
}
