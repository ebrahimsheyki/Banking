package com.ebrahimsheyki.banking.deposit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DepositRequest(

    @NotBlank
    @Size(max = 20)
    String accountNumber,

    @NotNull
    @DecimalMin(value = "0.0001")
    @Digits(integer = 15, fraction = 4)
    BigDecimal amount

) {

}
