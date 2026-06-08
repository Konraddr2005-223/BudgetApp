package com.zadsoft.controller;

import com.zadsoft.dto.TransactionCreationResult;
import com.zadsoft.dto.TransactionDto;
import com.zadsoft.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", exposedHeaders = "X-Budget-Warning")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category) {

        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59, 999999999) : null;

        return ResponseEntity.ok(transactionService.getFilteredTransactions(fromDateTime, toDateTime, category));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> addTransaction(@Valid @RequestBody TransactionDto dto) {
        TransactionCreationResult result = transactionService.addTransaction(dto);
        
        HttpHeaders headers = new HttpHeaders();
        if (result.getWarning() != null) {
            // Encode the warning message so it doesn't violate HTTP header character limitations
            String encodedWarning = URLEncoder.encode(result.getWarning(), StandardCharsets.UTF_8);
            headers.add("X-Budget-Warning", encodedWarning);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(headers)
                .body(result.getTransaction());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
