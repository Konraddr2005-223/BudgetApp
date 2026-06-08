package com.zadsoft.controller;

import com.zadsoft.dto.AccountDto;
import com.zadsoft.dto.TransactionDto;
import com.zadsoft.model.TransactionType;
import com.zadsoft.service.AccountService;
import com.zadsoft.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private TransactionService transactionService;

    @Test
    void testExportTransactionsToCsvReturnsCorrectFormat() throws Exception {
        // Arrange
        Long accountId = 1L;
        AccountDto account = AccountDto.builder().id(accountId).name("Konto Testowe").balance(BigDecimal.TEN).build();
        TransactionDto tx = TransactionDto.builder()
                .id(100L)
                .amount(new BigDecimal("15.50"))
                .type(TransactionType.EXPENSE)
                .category("Jedzenie")
                .description("Lunch")
                .transactionDate(LocalDateTime.of(2026, 6, 8, 12, 0, 0))
                .accountId(accountId)
                .build();

        when(accountService.getAccountDetails(accountId)).thenReturn(account);
        when(transactionService.getTransactionsForAccount(accountId)).thenReturn(Collections.singletonList(tx));

        // Act
        MvcResult result = mockMvc.perform(get("/api/accounts/{id}/transactions/export", accountId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        // Assert content contains UTF-8 BOM, headers, and rows
        String csvContent = result.getResponse().getContentAsString();
        
        // UTF-8 BOM is '\uFEFF'
        assertTrue(csvContent.startsWith("\uFEFF"), "CSV output should start with UTF-8 BOM");
        assertTrue(csvContent.contains("ID;Kwota;Typ;Kategoria;Opis;Data"), "CSV should contain headers");
        assertTrue(csvContent.contains("100;15.50;EXPENSE;Jedzenie;Lunch;2026-06-08 12:00:00"), "CSV should contain transaction data row");
    }
}
