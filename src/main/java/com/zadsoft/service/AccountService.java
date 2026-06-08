package com.zadsoft.service;

import com.zadsoft.dto.AccountDto;
import com.zadsoft.exception.BusinessConflictException;
import com.zadsoft.exception.ResourceNotFoundException;
import com.zadsoft.model.Account;
import com.zadsoft.repository.AccountRepository;
import com.zadsoft.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDto createAccount(AccountDto dto) {
        Account account = Account.builder()
                .name(dto.getName())
                .balance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO)
                .build();
        Account savedAccount = accountRepository.save(account);
        return convertToDto(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountDetails(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Konto o podanym ID nie istnieje: " + id));
        return convertToDto(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Konto o podanym ID nie istnieje: " + id));

        if (transactionRepository.existsByAccountId(id)) {
            throw new BusinessConflictException("Nie można usunąć konta, ponieważ posiada ono powiązane transakcje.");
        }

        accountRepository.delete(account);
    }

    private AccountDto convertToDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .name(account.getName())
                .balance(account.getBalance())
                .build();
    }
}
