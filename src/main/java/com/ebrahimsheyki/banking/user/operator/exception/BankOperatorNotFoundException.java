package com.ebrahimsheyki.banking.user.operator.exception;

public final class BankOperatorNotFoundException extends RuntimeException {

  public BankOperatorNotFoundException() {
    super("The requested bank operator was not found.");
  }
}
