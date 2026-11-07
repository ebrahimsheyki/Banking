package com.ebrahimsheyki.banking.withdrawal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WithdrawalResponse(
    UUID id,
    String accountNumber,
    BigDecimal amount,
    String currencyCode,
    WithdrawalStatus status,
    Instant createdAt
) {

  public static WithdrawalResponse from(Withdrawal withdrawal) {
    return new WithdrawalResponse(
        withdrawal.getId(),
        withdrawal.getAccountNumber(),
        withdrawal.getAmount(),
        withdrawal.getCurrencyCode(),
        withdrawal.getStatus(),
        withdrawal.getCreatedAt()
    );
  }
}
