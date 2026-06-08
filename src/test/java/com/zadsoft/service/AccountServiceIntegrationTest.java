package com.zadsoft.service;

import com.zadsoft.dto.AccountDto;
import com.zadsoft.dto.TransactionDto;
import com.zadsoft.exception.BusinessConflictException;
import com.zadsoft.model.Account;
import com.zadsoft.model.TransactionType;
import com.zadsoft.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void testDeleteAccountThrowsConflictExceptionIfTransactionsExist() {
        // Arrange: Create account
        AccountDto account = accountService.createAccount(AccountDto.builder().name("Konto z transakcją").balance(BigDecimal.TEN).build());
        
        // Add transaction
        TransactionDto txDto = TransactionDto.builder()
                .accountId(account.getId())
                .amount(BigDecimal.ONE)
                .type(TransactionType.EXPENSE)
                .category("Inne")
                .transactionDate(LocalDateTime.now())
                .build();
        transactionService.addTransaction(txDto);

        // Act & Assert: deleting should throw BusinessConflictException
        assertThrows(BusinessConflictException.class, () -> accountService.deleteAccount(account.getId()));

        // Clean up: delete transaction first, then delete account
        // Find transactions and delete
        var txs = transactionService.getTransactionsForAccount(account.getId());
        txs.forEach(t -> transactionService.deleteTransaction(t.getId()));
        assertDoesNotThrow(() -> accountService.deleteAccount(account.getId()));
    }

    @Test
    void testPessimisticWriteLockingBlocksConcurrentUpdates() throws InterruptedException {
        // Arrange: Create account
        Account account = Account.builder().name("Lock Test Account").balance(new BigDecimal("100.00")).build();
        account = accountRepository.save(account);
        final Long accountId = account.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch thread1Started = new CountDownLatch(1);
        CountDownLatch thread2Completed = new CountDownLatch(1);
        
        AtomicLong thread2AcquireTime = new AtomicLong();
        AtomicLong thread1ReleaseTime = new AtomicLong();

        // Thread 1: Acquire lock and hold it for 1 second
        executor.submit(() -> {
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
            try {
                // Get with pessimistic write lock
                accountRepository.findByIdWithLock(accountId).orElseThrow();
                thread1Started.countDown(); // Let Thread 2 try to acquire lock
                
                Thread.sleep(1000); // Hold lock for 1 second
                
                thread1ReleaseTime.set(System.currentTimeMillis());
                transactionManager.commit(status);
            } catch (Exception e) {
                transactionManager.rollback(status);
                fail("Thread 1 failed: " + e.getMessage());
            }
        });

        // Thread 2: Try to acquire lock. It must block until Thread 1 commits.
        executor.submit(() -> {
            try {
                thread1Started.await(); // Wait until Thread 1 has acquired the lock
                // Thread 2 starts transaction and tries to lock
                TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                try {
                    accountRepository.findByIdWithLock(accountId).orElseThrow();
                    thread2AcquireTime.set(System.currentTimeMillis());
                    transactionManager.commit(status);
                } finally {
                    thread2Completed.countDown();
                }
            } catch (Exception e) {
                fail("Thread 2 failed: " + e.getMessage());
            }
        });

        thread2Completed.await(); // Wait for both threads to finish
        executor.shutdown();

        // Verify that Thread 2 acquired the lock AFTER Thread 1 released/committed it
        assertTrue(thread2AcquireTime.get() >= thread1ReleaseTime.get(),
                "Thread 2 should have blocked until Thread 1 released the lock");
    }
}
