package com.payflow.transfer;

import com.payflow.account.Account;
import com.payflow.account.AccountRepository;
import com.payflow.exception.BusinessException;
import com.payflow.exception.ErrorCode;
import com.payflow.idempotency.IdempotencyKey;
import com.payflow.idempotency.IdempotencyKeyRepository;
import com.payflow.user.User;
import com.payflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class TransferIdempotencyTest {
    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private Long fromAccountId;
    private Long toAccountId;

    @BeforeEach
    void setUp(){
        idempotencyKeyRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        User sender = userRepository.save(new User("sender"));
        User receiver = userRepository.save(new User("receiver"));

        Account fromAccount = accountRepository.save(new Account("111-111",sender));
        fromAccount.deposit(10000L);
        fromAccount = accountRepository.save(fromAccount);

        Account toAccount = accountRepository.save(new Account("222-222",receiver));
        toAccount =accountRepository.save(toAccount);

        fromAccountId = fromAccount.getId();
        toAccountId = toAccount.getId();
    }


    @Test
    void 동일한_송금_요청을_두번_보내면_현재는_두번_처리된다(){
        //given
        Long amount = 3000L;

        //when
        transferService.transfer(fromAccountId, toAccountId,amount);
        transferService.transfer(fromAccountId, toAccountId, amount);

        Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow();
        Account toAccount = accountRepository.findById(toAccountId).orElseThrow();
        assertThat(fromAccount.getBalance()).isEqualTo(4000L);
        assertThat(toAccount.getBalance()).isEqualTo(6000L);
        assertThat(transferRepository.count()).isEqualTo(2);
    }

    @Test
    void 동일한_멱등성_키로_송금을_두번_요청하면_한번만_처리된다() {
        Long amount =3000L;
        String idempotencyKey ="transfer-test-001";

        transferService.transfer(fromAccountId,toAccountId,amount,idempotencyKey);
        transferService.transfer(fromAccountId,toAccountId,amount,idempotencyKey);

        Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow();
        Account toAccount = accountRepository.findById(toAccountId).orElseThrow();

        assertThat(fromAccount.getBalance()).isEqualTo(7000L);
        assertThat(toAccount.getBalance()).isEqualTo(3000L);

        assertThat(transferRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
    }
    @Test
    void 같은_멱등성_키로_다른_송금_요청을_보내면_예외가_발생한다() {
        String idempotencyKey ="same-key-test";

        transferService.transfer(fromAccountId,toAccountId,3000L, idempotencyKey);
        BusinessException exception = assertThrows(BusinessException.class, ()->transferService.transfer(fromAccountId,toAccountId,5000L,idempotencyKey));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);

        Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow();

        Account toAccount = accountRepository.findById(toAccountId).orElseThrow();

        assertThat(fromAccount.getBalance()).isEqualTo(7000L);

        assertThat(toAccount.getBalance()).isEqualTo(3000L);

        assertThat(transferRepository.count()).isEqualTo(1);

        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
    }

    @Test
    void 동일한_멱등성_키로_동시에_송금을_요청해도_한번만_처리된다() throws Exception {
        Long amount = 3000L;
        String idempotencyKey ="concurrent-test-key";

        int threadCount=2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for(int i=0;i<threadCount;i++){
            executorService.submit(()->{
                try{
                    readyLatch.countDown();
                    startLatch.await();

                    transferService.transfer(fromAccountId,toAccountId,amount,idempotencyKey);
                    successCount.incrementAndGet();
                }catch (Exception e){
                    failureCount.incrementAndGet();
                }finally {
                    doneLatch.countDown();
                }
            });
        }
        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executorService.shutdown();

        Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow();
        Account toAccount = accountRepository.findById(toAccountId).orElseThrow();
        assertThat(fromAccount.getBalance()).isEqualTo(7000L);
        assertThat(toAccount.getBalance()).isEqualTo(3000L);

        assertThat(transferRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failureCount.get()).isEqualTo(0);
    }
}
