package com.zadsoft.repository;

import com.zadsoft.model.Account;
import com.zadsoft.model.Transaction;
import com.zadsoft.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .name("Konto Testowe Repozytorium")
                .balance(new BigDecimal("1000.00"))
                .build();
        account = accountRepository.save(account);

        // Save some dummy transactions
        transactionRepository.save(Transaction.builder()
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.EXPENSE)
                .category("Jedzenie")
                .transactionDate(LocalDateTime.of(2026, 6, 1, 10, 0))
                .account(account)
                .build());

        transactionRepository.save(Transaction.builder()
                .amount(new BigDecimal("350.00"))
                .type(TransactionType.EXPENSE)
                .category("Transport")
                .transactionDate(LocalDateTime.of(2026, 6, 5, 12, 0))
                .account(account)
                .build());

        transactionRepository.save(Transaction.builder()
                .amount(new BigDecimal("5000.00"))
                .type(TransactionType.INCOME)
                .category("Wynagrodzenie")
                .transactionDate(LocalDateTime.of(2026, 6, 7, 9, 0))
                .account(account)
                .build());
    }

    @Test
    void testFindFilteredTransactionsByCategory() {
        List<Transaction> results = transactionRepository.findFilteredTransactions(null, null, "jedzenie");
        assertEquals(1, results.size());
        assertEquals(new BigDecimal("150.00"), results.get(0).getAmount());
    }

    @Test
    void testFindFilteredTransactionsByDateRange() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 2, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 8, 23, 59);

        List<Transaction> results = transactionRepository.findFilteredTransactions(from, to, null);
        assertEquals(2, results.size()); // Transport & Wynagrodzenie
    }

    @Test
    void testSumByCategoryAndTypeAndDateRange() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 30, 23, 59);

        BigDecimal totalExpenses = transactionRepository.sumByCategoryAndTypeAndDateRange("Jedzenie", TransactionType.EXPENSE, start, end);
        assertEquals(new BigDecimal("150.00"), totalExpenses);

        BigDecimal nonExistentSum = transactionRepository.sumByCategoryAndTypeAndDateRange("Rozrywka", TransactionType.EXPENSE, start, end);
        assertEquals(BigDecimal.ZERO, nonExistentSum); // COALESCE returns 0
    }
}
