package com.ebrahimsheyki.banking.user.operator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BankOperatorResponse(
    UUID id,
    UUID userId,
    String username,
    BankOperatorStatus status,
    Instant createdAt,
    Instant updatedAt
) {

  public static BankOperatorResponse from(BankOperator bankOperator) {

    Objects.requireNonNull(
        bankOperator,
        "bankOperator must not be null"
    );

    return new BankOperatorResponse(
        bankOperator.getId(),
        bankOperator.getUserId(),
        bankOperator.getUsername(),
        bankOperator.getStatus(),
        bankOperator.getCreatedAt(),
        bankOperator.getUpdatedAt()
    );
  }
}
