package com.ebrahimsheyki.banking.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
    UUID id,
    String sourceAccountNumber,
    String destinationAccountNumber,
    BigDecimal amount,
    String currencyCode,
    TransferStatus status,
    Instant createdAt
) {

  public static TransferResponse from(Transfer transfer) {
    return new TransferResponse(
        transfer.getId(),
        transfer.getSourceAccountNumber(),
        transfer.getDestinationAccountNumber(),
        transfer.getAmount(),
        transfer.getCurrencyCode(),
        transfer.getStatus(),
        transfer.getCreatedAt()
    );
  }
}
