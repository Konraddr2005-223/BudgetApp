package com.zadsoft.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nazwa konta nie może być pusta")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Saldo konta nie może być puste")
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
}
