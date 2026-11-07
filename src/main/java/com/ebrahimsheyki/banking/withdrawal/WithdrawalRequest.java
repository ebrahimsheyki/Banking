package com.ebrahimsheyki.banking.withdrawal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WithdrawalRequest(

    @NotBlank
    @Size(max = 20)
    String accountNumber,

    @NotNull
    @DecimalMin("0.0001")
    @Digits(integer = 15, fraction = 4)
    BigDecimal amount

) {

}
