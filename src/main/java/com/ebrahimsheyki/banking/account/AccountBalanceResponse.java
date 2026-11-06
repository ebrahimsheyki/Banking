package com.ebrahimsheyki.banking.account;

import java.math.BigDecimal;
import java.util.Objects;

public record AccountBalanceResponse(
    String accountNumber,
    BigDecimal balance,
    String currencyCode,
    AccountStatus status
) {

  public AccountBalanceResponse {
    Objects.requireNonNull(accountNumber, "accountNumber must not be null");
    Objects.requireNonNull(balance, "balance must not be null");
    Objects.requireNonNull(currencyCode, "currencyCode must not be null");
    Objects.requireNonNull(status, "status must not be null");
  }

  public static AccountBalanceResponse from(Account account) {
    Objects.requireNonNull(account, "account must not be null");

    return new AccountBalanceResponse(
        account.getAccountNumber(),
        account.getBalance(),
        account.getCurrencyCode(),
        account.getStatus()
    );
  }
}