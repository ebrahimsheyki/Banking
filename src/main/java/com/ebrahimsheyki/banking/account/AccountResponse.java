package com.ebrahimsheyki.banking.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(UUID id, String accountNumber, AccountType accountType,
                              String currencyCode, BigDecimal balance, AccountStatus status,
                              Instant createdAt, Instant updatedAt) {

  public static AccountResponse from(Account account) {
    return new AccountResponse(account.getId(), account.getAccountNumber(),
        account.getAccountType(), account.getCurrencyCode(), account.getBalance(),
        account.getStatus(), account.getCreatedAt(), account.getUpdatedAt());
  }
}