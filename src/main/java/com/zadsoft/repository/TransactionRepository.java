package com.zadsoft.repository;

import com.zadsoft.model.Transaction;
import com.zadsoft.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByAccountId(Long accountId);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a WHERE " +
           "(:from IS NULL OR t.transactionDate >= :from) AND " +
           "(:to IS NULL OR t.transactionDate <= :to) AND " +
           "(:category IS NULL OR LOWER(t.category) = LOWER(:category)) " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findFilteredTransactions(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("category") String category);

    List<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
           "LOWER(t.category) = LOWER(:category) AND t.type = :type AND " +
           "t.transactionDate >= :start AND t.transactionDate <= :end")
    BigDecimal sumByCategoryAndTypeAndDateRange(
            @Param("category") String category,
            @Param("type") TransactionType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
