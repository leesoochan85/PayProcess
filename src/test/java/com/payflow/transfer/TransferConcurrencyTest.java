package com.payflow.transfer;

import com.payflow.account.Account;
import com.payflow.account.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TransferConcurrencyTest {
    @Autowired
    TransferService transferService;

    @Autowired
    AccountRepository accountRepository;

    Account fromAccount;
    Account toAccount1;
    Account toAccount2;

    @BeforeEach
    void setUp(){
        accountRepository.deleteAll();

        fromAccount = new Account("111-111",null);
        toAccount1 = new Account("222-222", null);
        toAccount2 = new Account("333-333", null);

        fromAccount.deposit(10_000L);

        fromAccount=accountRepository.save(fromAccount);
        toAccount1 = accountRepository.save(toAccount1);
        toAccount2 = accountRepository.save(toAccount2);
    }


    @Test
    void 동시에_같은_계좌에서_송금한다() throws InterruptedException{

        int threadCount=2;

        AtomicInteger successCount =new AtomicInteger();
        AtomicInteger failCount =new AtomicInteger();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch latch = new CountDownLatch(threadCount);

        Long fromAccountId = fromAccount.getId();
        Long toAccountId1 = toAccount1.getId();
        Long toAccountId2 = toAccount2.getId();

        executorService.submit(()->{
            try{
                transferService.transfer(fromAccountId,toAccountId1,8_000L);
                successCount.incrementAndGet();
            }catch(Exception e){
                failCount.incrementAndGet();
                System.out.println(
                        "실패 예외 타입: " + e.getClass().getName()
                );

                System.out.println(
                        "실패 메시지: " + e.getMessage()
                );
            }finally {
                latch.countDown();
            }
        });
        executorService.submit(()->{
            try{
                transferService.transfer(fromAccountId,toAccountId2,8_000L);
                successCount.incrementAndGet();
            }catch(Exception e){
                failCount.incrementAndGet();
                System.out.println(
                        "실패 예외 타입: " + e.getClass().getName()
                );

                System.out.println(
                        "실패 메시지: " + e.getMessage()
                );
            }finally {
                latch.countDown();
            }
        });
        latch.await();
        executorService.shutdown();

        Account resultFrom = accountRepository.findById(fromAccountId).orElseThrow();
        Account resultTo1 = accountRepository.findById(toAccountId1).orElseThrow();
        Account resultTo2 = accountRepository.findById(toAccountId2).orElseThrow();

        System.out.println("출금 계좌 잔액: " + resultFrom.getBalance());
        System.out.println("입금 계좌1 잔액: " + resultTo1.getBalance());
        System.out.println("입금 계좌2 잔액: " + resultTo2.getBalance());

        long totalBalance = resultFrom.getBalance()+resultTo1.getBalance()+resultTo2.getBalance();

        assertThat(resultFrom.getBalance()).isEqualTo(2_000L);
        assertThat(resultTo1.getBalance() + resultTo2.getBalance()).isEqualTo(8_000L);
        assertThat(totalBalance).isEqualTo(10_000L);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }
}
