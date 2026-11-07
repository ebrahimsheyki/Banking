package com.ebrahimsheyki.banking.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccountTransferRequest(

    @NotBlank(message = "sourceAccountNumber is required")
    @Size(max = 20, message = "sourceAccountNumber is too long")
    String sourceAccountNumber,

    @NotBlank(message = "destinationAccountNumber is required")
    @Size(max = 20, message = "destinationAccountNumber is too long")
    String destinationAccountNumber,

    @NotNull(message = "amount is required")
    @DecimalMin(
        value = "0.0",
        inclusive = false,
        message = "amount must be positive"
    )
    @Digits(
        integer = 15,
        fraction = 4,
        message = "amount exceeds the supported precision"
    )
    BigDecimal amount

) {

}
