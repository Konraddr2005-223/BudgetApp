package com.zadsoft.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "category_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Kategoria nie może być pusta")
    @Column(nullable = false, unique = true)
    private String category;

    @NotNull(message = "Limit miesięczny jest wymagany")
    @Positive(message = "Limit musi być większy niż zero")
    @Column(name = "monthly_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyLimit;
}
