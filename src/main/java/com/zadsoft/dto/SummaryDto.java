package com.zadsoft.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryDto {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private Map<String, BigDecimal> expensesByCategory;
}
