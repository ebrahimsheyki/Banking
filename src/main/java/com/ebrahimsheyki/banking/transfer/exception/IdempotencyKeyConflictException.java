package com.ebrahimsheyki.banking.transfer.exception;

public final class IdempotencyKeyConflictException extends RuntimeException {

  public IdempotencyKeyConflictException() {
    super("The Idempotency-Key has already been used for a different request.");
  }
}
