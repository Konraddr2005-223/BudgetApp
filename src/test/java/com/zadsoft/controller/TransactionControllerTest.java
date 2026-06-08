package com.zadsoft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zadsoft.dto.TransactionCreationResult;
import com.zadsoft.dto.TransactionDto;
import com.zadsoft.model.TransactionType;
import com.zadsoft.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void testGetTransactionsReturnsList() throws Exception {
        TransactionDto tx = TransactionDto.builder()
                .id(1L)
                .amount(BigDecimal.TEN)
                .type(TransactionType.INCOME)
                .category("Praca")
                .transactionDate(LocalDateTime.of(2026, 6, 8, 12, 0))
                .accountId(1L)
                .accountName("Konto Główne")
                .build();

        when(transactionService.getFilteredTransactions(any(), any(), any()))
                .thenReturn(Collections.singletonList(tx));

        mockMvc.perform(get("/api/transactions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].amount").value(10.00))
                .andExpect(jsonPath("$[0].category").value("Praca"))
                .andExpect(jsonPath("$[0].type").value("INCOME"));
    }

    @Test
    void testAddTransactionReturnsCreatedWithHeaderIfWarningExists() throws Exception {
        TransactionDto request = TransactionDto.builder()
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.EXPENSE)
                .category("Rozrywka")
                .transactionDate(LocalDateTime.of(2026, 6, 8, 12, 0))
                .accountId(1L)
                .build();

        TransactionDto responseDto = TransactionDto.builder()
                .id(99L)
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.EXPENSE)
                .category("Rozrywka")
                .transactionDate(LocalDateTime.of(2026, 6, 8, 12, 0))
                .accountId(1L)
                .accountName("Konto Główne")
                .build();

        String warningMsg = "Ostrzeżenie: Przekroczono limit budżetu dla kategorii 'Rozrywka'";
        TransactionCreationResult result = new TransactionCreationResult(responseDto, warningMsg);

        when(transactionService.addTransaction(any(TransactionDto.class))).thenReturn(result);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Budget-Warning", URLEncoder.encode(warningMsg, StandardCharsets.UTF_8)))
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.category").value("Rozrywka"));
    }

    @Test
    void testAddTransactionReturnsBadRequestIfValidationFails() throws Exception {
        // Amount must be positive (> 0)
        TransactionDto request = TransactionDto.builder()
                .amount(new BigDecimal("-50.00")) // Invalid
                .type(TransactionType.EXPENSE)
                .category("") // Invalid (blank)
                .transactionDate(LocalDateTime.now())
                .accountId(1L)
                .build();

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Błędy walidacji danych wejściowych"))
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.category").exists());
    }
}
