package com.ebrahimsheyki.banking.account.exception;

public final class InsufficientFundsException extends RuntimeException {

  public InsufficientFundsException() {
    super("Insufficient funds.");
  }
}