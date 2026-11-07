package com.ebrahimsheyki.banking.user.operator;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BankOperatorAssignmentRequest(

    @NotNull(message = "userId must not be null")
    UUID userId

) {

}
