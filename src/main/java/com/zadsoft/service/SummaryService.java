package com.zadsoft.service;

import com.zadsoft.dto.SummaryDto;
import com.zadsoft.model.Transaction;
import com.zadsoft.model.TransactionType;
import com.zadsoft.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public SummaryDto getSummary(LocalDateTime from, LocalDateTime to) {
        List<Transaction> transactions = transactionRepository.findFilteredTransactions(from, to, null);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        Map<String, BigDecimal> expensesByCategory = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
            } else if (t.getType() == TransactionType.EXPENSE) {
                totalExpenses = totalExpenses.add(t.getAmount());
                
                String category = t.getCategory();
                BigDecimal currentVal = expensesByCategory.getOrDefault(category, BigDecimal.ZERO);
                expensesByCategory.put(category, currentVal.add(t.getAmount()));
            }
        }

        return SummaryDto.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .expensesByCategory(expensesByCategory)
                .build();
    }
}
