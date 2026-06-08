package com.zadsoft.service;

import com.zadsoft.dto.TransactionCreationResult;
import com.zadsoft.dto.TransactionDto;
import com.zadsoft.exception.ResourceNotFoundException;
import com.zadsoft.model.Account;
import com.zadsoft.model.CategoryLimit;
import com.zadsoft.model.Transaction;
import com.zadsoft.model.TransactionType;
import com.zadsoft.repository.AccountRepository;
import com.zadsoft.repository.CategoryLimitRepository;
import com.zadsoft.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryLimitRepository categoryLimitRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .name("Konto Testowe")
                .balance(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void testAddIncomeTransactionUpdatesBalanceCorrectly() {
        // Arrange
        TransactionDto dto = TransactionDto.builder()
                .accountId(1L)
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.INCOME)
                .category("Wynagrodzenie")
                .transactionDate(LocalDateTime.now())
                .build();

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(100L);
            return tx;
        });

        // Act
        TransactionCreationResult result = transactionService.addTransaction(dto);

        // Assert
        assertEquals(new BigDecimal("1200.00"), testAccount.getBalance());
        assertEquals(100L, result.getTransaction().getId());
        assertNull(result.getWarning());
        verify(accountRepository, times(1)).save(testAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testAddExpenseTransactionUpdatesBalanceCorrectly() {
        // Arrange
        TransactionDto dto = TransactionDto.builder()
                .accountId(1L)
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.EXPENSE)
                .category("Zakupy")
                .transactionDate(LocalDateTime.now())
                .build();

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransactionCreationResult result = transactionService.addTransaction(dto);

        // Assert
        assertEquals(new BigDecimal("850.00"), testAccount.getBalance());
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void testAddTransactionThrowsExceptionIfAccountNotFound() {
        // Arrange
        TransactionDto dto = TransactionDto.builder()
                .accountId(99L)
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.EXPENSE)
                .category("Zakupy")
                .build();

        when(accountRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.addTransaction(dto));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testCategoryLimitWarningTriggersOnExceededLimit() {
        // Arrange
        LocalDateTime txDate = LocalDateTime.of(2026, 6, 7, 12, 0);
        TransactionDto dto = TransactionDto.builder()
                .accountId(1L)
                .amount(new BigDecimal("300.00"))
                .type(TransactionType.EXPENSE)
                .category("Jedzenie")
                .transactionDate(txDate)
                .build();

        CategoryLimit limit = CategoryLimit.builder()
                .id(1L)
                .category("Jedzenie")
                .monthlyLimit(new BigDecimal("500.00"))
                .build();

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryLimitRepository.findByCategoryIgnoreCase("Jedzenie")).thenReturn(Optional.of(limit));
        
        // Sum spent in the month (including new tx or existing total is 600.00 which exceeds 500.00)
        when(transactionRepository.sumByCategoryAndTypeAndDateRange(
                eq("Jedzenie"), eq(TransactionType.EXPENSE), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("600.00"));

        // Act
        TransactionCreationResult result = transactionService.addTransaction(dto);

        // Assert
        assertNotNull(result.getWarning());
        assertTrue(result.getWarning().contains("Przekroczono limit"));
    }

    @Test
    void testDeleteTransactionRollsBackBalance() {
        // Arrange
        Transaction tx = Transaction.builder()
                .id(100L)
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.EXPENSE)
                .category("Zakupy")
                .account(testAccount)
                .build();

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(tx));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testAccount));

        // Act
        transactionService.deleteTransaction(100L);

        // Assert
        assertEquals(new BigDecimal("1150.00"), testAccount.getBalance());
        verify(transactionRepository, times(1)).delete(tx);
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void testAddExpenseTransactionAllowsNegativeBalance() {
        // Arrange
        TransactionDto dto = TransactionDto.builder()
                .accountId(1L)
                .amount(new BigDecimal("1200.00"))
                .type(TransactionType.EXPENSE)
                .category("Zakupy")
                .transactionDate(LocalDateTime.now())
                .build();

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransactionCreationResult result = transactionService.addTransaction(dto);

        // Assert
        assertEquals(new BigDecimal("-200.00"), testAccount.getBalance());
        verify(accountRepository, times(1)).save(testAccount);
    }
}
