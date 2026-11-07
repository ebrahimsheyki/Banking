package com.ebrahimsheyki.banking.deposit.exception;

public final class DepositAccountNotFoundException extends RuntimeException {

  public DepositAccountNotFoundException() {
    super("The requested account was not found.");
  }
}
