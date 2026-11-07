package com.ebrahimsheyki.banking.transfer.exception;

public final class InvalidIdempotencyKeyException extends RuntimeException {

  public InvalidIdempotencyKeyException() {
    super("A valid Idempotency-Key is required.");
  }
}
