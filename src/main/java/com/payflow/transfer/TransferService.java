package com.payflow.transfer;

import com.payflow.account.Account;
import com.payflow.account.AccountRepository;
import com.payflow.exception.BusinessException;
import com.payflow.exception.ErrorCode;
import com.payflow.idempotency.IdempotencyKey;
import com.payflow.idempotency.IdempotencyKeyRepository;
import com.payflow.transfer.dto.AccountTransferResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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
        transferRepository.save(transfer);
    }


    public List<TransferResponse> findAll(){
        return transferRepository.findAll().stream().map(TransferResponse::from).toList();
    }

    public List<TransferResponse> findSentTransfers(Long accountId){
        return transferRepository.findByFromAccountId(accountId).stream().map(TransferResponse::from).toList();
    }

    public List<TransferResponse>findReceivedTransfers(Long accountId){
        return transferRepository.findByToAccountId(accountId).stream().map(TransferResponse::from).toList();
    }

    public List<AccountTransferResponse> findAccountTransfers(Long accountId) {
        return transferRepository.
                findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream().map(transfer -> AccountTransferResponse.from(
                        transfer,
                        accountId)).toList();
    }
}
