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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryLimitRepository categoryLimitRepository;

    @Transactional(readOnly = true)
    public List<TransactionDto> getFilteredTransactions(LocalDateTime from, LocalDateTime to, String category) {
        return transactionRepository.findFilteredTransactions(from, to, category).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionCreationResult addTransaction(TransactionDto dto) {
        // Lock account to prevent concurrent balance modifications
        Account account = accountRepository.findByIdWithLock(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Konto o podanym ID nie istnieje: " + dto.getAccountId()));

        // Update balance
        if (dto.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(dto.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(dto.getAmount()));
        }
        accountRepository.save(account);

        // Save transaction
        Transaction transaction = Transaction.builder()
                .amount(dto.getAmount())
                .type(dto.getType())
                .category(dto.getCategory().trim())
                .description(dto.getDescription())
                .transactionDate(dto.getTransactionDate())
                .account(account)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        // Check category budget limit
        String warning = null;
        if (dto.getType() == TransactionType.EXPENSE) {
            Optional<CategoryLimit> limitOpt = categoryLimitRepository.findByCategoryIgnoreCase(dto.getCategory().trim());
            if (limitOpt.isPresent()) {
                BigDecimal limitAmount = limitOpt.get().getMonthlyLimit();

                LocalDateTime start = dto.getTransactionDate().withDayOfMonth(1).with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime end = dto.getTransactionDate().with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

                BigDecimal currentMonthSpent = transactionRepository.sumByCategoryAndTypeAndDateRange(
                        dto.getCategory().trim(),
                        TransactionType.EXPENSE,
                        start,
                        end
                );

                if (currentMonthSpent.compareTo(limitAmount) > 0) {
                    warning = String.format("Ostrzeżenie: Przekroczono limit budżetu dla kategorii '%s'. Limit miesięczny: %s zł, Wydano w tym miesiącu: %s zł.",
                            limitOpt.get().getCategory(), limitAmount, currentMonthSpent);
                }
            }
        }

        return new TransactionCreationResult(convertToDto(saved), warning);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transakcja o podanym ID nie istnieje: " + id));

        // Lock account to prevent concurrent balance modifications
        Account account = accountRepository.findByIdWithLock(transaction.getAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Konto powiązane z transakcją nie istnieje: " + transaction.getAccount().getId()));

        // Rollback balance
        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }
        accountRepository.save(account);

        // Delete transaction
        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsForAccount(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Konto o podanym ID nie istnieje: " + accountId);
        }
        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private TransactionDto convertToDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .accountId(transaction.getAccount().getId())
                .accountName(transaction.getAccount().getName())
                .build();
    }
}
