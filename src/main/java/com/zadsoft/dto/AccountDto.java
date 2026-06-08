package com.zadsoft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDto {

    private Long id;

    @NotBlank(message = "Nazwa konta jest wymagana")
    private String name;

    private BigDecimal balance;
}
