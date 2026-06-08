package com.zadsoft.controller;

import com.zadsoft.dto.AccountDto;
import com.zadsoft.dto.TransactionDto;
import com.zadsoft.service.AccountService;
import com.zadsoft.service.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody AccountDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountDetails(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountDetails(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/transactions/export")
    public void exportTransactionsToCsv(@PathVariable Long id, HttpServletResponse response) throws IOException {
        AccountDto account = accountService.getAccountDetails(id);
        List<TransactionDto> transactions = transactionService.getTransactionsForAccount(id);

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        
        String safeFileName = URLEncoder.encode("transakcje_" + account.getName().replace(" ", "_") + ".csv", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + safeFileName);

        PrintWriter writer = response.getWriter();
        // Write UTF-8 BOM so Excel opens it with correct encoding
        writer.write('\uFEFF');
        
        // CSV Header
        writer.println("ID;Kwota;Typ;Kategoria;Opis;Data");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TransactionDto t : transactions) {
            String desc = t.getDescription() != null ? t.getDescription().replace(";", " ") : "";
            writer.println(String.format(java.util.Locale.US, "%d;%.2f;%s;%s;%s;%s",
                    t.getId(),
                    t.getAmount(),
                    t.getType().name(),
                    t.getCategory().replace(";", " "),
                    desc,
                    t.getTransactionDate().format(formatter)
            ));
        }
        writer.flush();
    }
}
