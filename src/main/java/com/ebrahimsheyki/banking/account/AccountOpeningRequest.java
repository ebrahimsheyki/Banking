package com.ebrahimsheyki.banking.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AccountOpeningRequest(
    @NotNull(message = "accountType is required") AccountType accountType,

    @NotBlank(message = "currencyCode is required") @Pattern(regexp = "(?i)^[A-Z]{3}$",
        message = "currencyCode must contain three letters") String currencyCode) {}