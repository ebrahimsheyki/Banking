package com.ebrahimsheyki.banking.deposit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositResponse(
    UUID id,
    String accountNumber,
    BigDecimal amount,
    String currencyCode,
    DepositStatus status,
    Instant createdAt
) {

  public static DepositResponse from(Deposit deposit) {
    return new DepositResponse(
        deposit.getId(),
        deposit.getAccountNumber(),
        deposit.getAmount(),
        deposit.getCurrencyCode(),
        deposit.getStatus(),
        deposit.getCreatedAt()
    );
  }
}
