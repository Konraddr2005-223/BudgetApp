package com.zadsoft.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionCreationResult {
    private final TransactionDto transaction;
    private final String warning;
}
