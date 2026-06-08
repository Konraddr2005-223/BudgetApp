package com.zadsoft.dto;

import com.zadsoft.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {

    private Long id;

    @NotNull(message = "Kwota jest wymagana")
    @Positive(message = "Kwota musi być większa od zera")
    private BigDecimal amount;

    @NotNull(message = "Typ transakcji jest wymagany (INCOME/EXPENSE)")
    private TransactionType type;

    @NotBlank(message = "Kategoria jest wymagana")
    private String category;

    private String description;

    @NotNull(message = "Data transakcji jest wymagana")
    private LocalDateTime transactionDate;

    @NotNull(message = "Identyfikator konta jest wymagany")
    private Long accountId;

    private String accountName;
}
