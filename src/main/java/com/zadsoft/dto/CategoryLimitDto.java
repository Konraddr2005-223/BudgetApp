package com.zadsoft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryLimitDto {

    private Long id;

    @NotBlank(message = "Kategoria jest wymagana")
    private String category;

    @NotNull(message = "Limit jest wymagany")
    @Positive(message = "Limit musi być większy od zera")
    private BigDecimal monthlyLimit;
}
