package com.ebrahimsheyki.banking.transfer.exception;

public final class TransferCurrencyMismatchException extends RuntimeException {

  public TransferCurrencyMismatchException() {
    super("Source and destination accounts must use the same currency.");
  }
}
